import fi.vauhtijuoksu.utilities.bashCommand
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.LocalPortForward
import org.jacoco.core.data.ExecutionDataWriter
import org.jacoco.core.runtime.RemoteControlReader
import org.jacoco.core.runtime.RemoteControlWriter
import java.io.FileOutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.random.Random

plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
}

val featureTests: Configuration by configurations.creating {
}

val buildId = Random.nextInt(0, Int.MAX_VALUE)

dependencies {
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("io.vertx:vertx-web-client")

    featureTests("org.jacoco:org.jacoco.agent:0.8.7:runtime")
}

val jacocoPath = "$buildDir/tmp/jacocoagent.jar"

tasks {
    /**
     * Find jacoco agent jar file path and copy it under build/tmp/jacocoagent.jar
     */
    val findJacoco by registering {
        dependsOn(featureTests)
        outputs.file(jacocoPath)
        doLast {
            val jacocoJarPath = featureTests.resolve().first {
                // Name org.jacoco.agent-NNN-runtime.jar
                it.name.matches(Regex("org.jacoco.agent-(\\d[.])+.-runtime.jar"))
            }.absolutePath
            exec {
                bashCommand("mkdir -p build/tmp && cp \"$jacocoJarPath\" $jacocoPath")
            }
        }
    }

    /**
     * Run block when forwarded to each vauhtijuoksu-api pod created in the current build
     */
    fun forwardToEachVauhtijuoksuPod(port: Int, block: (portForward: LocalPortForward) -> Unit) {
        DefaultKubernetesClient().use { k8s ->
            k8s.pods().inNamespace("default")
                .withLabel("app.kubernetes.io/name", "vauhtijuoksu-api")
                .withLabel("build", "$buildId")
                .list().items.forEach { pod ->
                    k8s.pods().inNamespace("default").withName(pod.metadata.name).portForward(port)
                        .use { portForward ->
                            logger.debug("Forwarding to pod ${pod.metadata.name} using local port ${portForward.localPort}")
                            block.invoke(portForward)
                        }
                }
        }
    }

    /**
     * Task to reset coverage recording for each pod in cluster.
     * Can be used to measure coverage for a single test class for example
     */
    val resetJacoco by registering {
        doLast {
            forwardToEachVauhtijuoksuPod(8091) { portForward ->
                Socket(portForward.localAddress, portForward.localPort).use { socket ->
                    val writer = RemoteControlWriter(socket.getOutputStream())
                    val remoteReader = RemoteControlReader(socket.getInputStream())

                    writer.visitDumpCommand(false, true)
                    // Not using writer causes handshake exception. No idea why
                    remoteReader.read()
                    logger.debug("Reset done")
                }
            }
        }
    }

    /**
     * Run a patched version of vauhtijuoksu-api that has jacoco agent running in a local cluster.
     */
    val clusterWithJacoco by registering {
        val patch = """
            spec:
              template:
                metadata:
                  labels:
                    build: "$buildId"
                spec:
                  containers:
                    - name: vauhtijuoksu-api
                      volumeMounts:
                        - name: jacoco
                          mountPath: /jacoco/
                      env:
                        - name: JAVA_OPTS
                          value: "-javaagent:/jacoco/jacocoagent.jar=destfile=/jacoco.exec,output=tcpserver,address=*,port=8091"
                  volumes:
                    - name: jacoco
                      configMap:
                        name: jacoco
        """.trimIndent()
        dependsOn(findJacoco, getByPath(":deployment:runInCluster"))
        doLast {
            exec {
                bashCommand(
                    """
                    kubectl delete cm jacoco --ignore-not-found=true
                    kubectl create cm jacoco --from-file  "$jacocoPath"
                    """.trimIndent()
                )
            }
            DefaultKubernetesClient().use { k8s ->
                logger.debug("Patching deployment")
                val vauhtijuoksuApiDeployment = k8s.apps().deployments().inNamespace("default").withName("vauhtijuoksu-api")
                vauhtijuoksuApiDeployment.patch(patch)
                vauhtijuoksuApiDeployment.waitUntilReady(30, TimeUnit.SECONDS)
                logger.debug("Deployment patched")
                // Wait for the pods to actually start before resetting. Can be removed if proper health check is implemented
                Thread.sleep(10000)
            }
        }
    }

    /**
     * Fetch jacoco coverage reports from each running pod in local cluster and place them under build/jacoco
     */
    val dumpJacoco by registering {
        doLast {
            var podIndex = 0
            forwardToEachVauhtijuoksuPod(8091) { portForward ->
                FileOutputStream("$buildDir/jacoco/test-pod-$podIndex.exec").use { file ->
                    val localWriter = ExecutionDataWriter(file)
                    Socket(portForward.localAddress, portForward.localPort).use { socket ->
                        logger.debug("Socket open")
                        val writer = RemoteControlWriter(socket.getOutputStream())
                        val remoteReader = RemoteControlReader(socket.getInputStream())

                        remoteReader.setSessionInfoVisitor(localWriter)
                        remoteReader.setExecutionDataVisitor(localWriter)

                        writer.visitDumpCommand(true, false)
                        remoteReader.read()
                        podIndex++
                        logger.debug("Dump done")
                    }
                }
            }
        }
    }

    test {
        description = "Run feature tests against local cluster and gather coverage with jacoco"
        outputs.upToDateWhen { false }
        dependsOn(clusterWithJacoco)
        finalizedBy(dumpJacoco)
    }

    jacocoTestReport {
        // No source code so no sense in generating a report here
        reports {
            csv.required.set(false)
            html.required.set(false)
        }
    }
}
