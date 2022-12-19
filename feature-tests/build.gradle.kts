import fi.vauhtijuoksu.utilities.bashCommand
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.LocalPortForward
import org.jacoco.agent.AgentJar
import org.jacoco.core.data.ExecutionDataWriter
import org.jacoco.core.runtime.RemoteControlReader
import org.jacoco.core.runtime.RemoteControlWriter
import java.io.FileOutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.random.Random

plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
    jacoco
}

val featureTests: Configuration by configurations.creating {
}

val buildId = Random.nextInt(0, Int.MAX_VALUE)

dependencies {
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("io.vertx:vertx-web-client")

    featureTests("org.jacoco:org.jacoco.agent:0.8.8:runtime")
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
            val agentJar: File = AgentJar.extractToTempLocation()
            exec {
                bashCommand("mkdir -p build/tmp && cp \"${agentJar.absolutePath}\" $jacocoPath")
            }
        }
    }

    /**
     * Run block when forwarded to each vauhtijuoksu-api pod created in the current build
     */
    fun forwardToEachVauhtijuoksuPod(port: Int, block: (portForward: LocalPortForward) -> Unit) {
        KubernetesClientBuilder().build().use { k8s ->
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
            KubernetesClientBuilder().build().use { k8s ->
                logger.debug("Patching deployment")
                val vauhtijuoksuApiDeployment = k8s.apps().deployments().inNamespace("default").withName("vauhtijuoksu-api")
                vauhtijuoksuApiDeployment.patch(patch)
                vauhtijuoksuApiDeployment.waitUntilReady(30, TimeUnit.SECONDS)
                logger.debug("Deployment patched")
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
        useJUnitPlatform()
        dependsOn(clusterWithJacoco)
        finalizedBy(dumpJacoco, jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        // No source code so no sense in generating a report here
        reports {
            csv.required.set(false)
            html.required.set(false)
        }
    }
}
