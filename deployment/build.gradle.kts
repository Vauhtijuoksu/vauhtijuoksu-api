import fi.vauhtijuoksu.utilities.bashCommand
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

plugins {
    id("vauhtijuoksu-api.kotlin-common-conventions")
}

val dockerImage: Configuration by configurations.creating {
}

dependencies {
    dockerImage(project(path = ":server", "dockerImageConfiguration"))
}

tasks {
    val lintCharts by registering {
        fun lintDir(chartDirectory: String, output: String) {
            val res = exec {
                bashCommand("helm lint $chartDirectory")
                standardOutput = FileOutputStream(output, true)
                isIgnoreExitValue = true
            }
            if (res.exitValue != 0) {
                throw GradleException("Helm lint failed. Check $output for details")
            }
        }

        description = "Lint helm charts"
        group = "Verification"
        inputs.dir("$projectDir/api-server")
        inputs.dir("$projectDir/mockserver")

        val mockserverOutput = "$buildDir/mockserver-lint-results.txt"
        outputs.file(mockserverOutput)
        val apiServerOutput = "$buildDir/api-server-lint-results.txt"
        outputs.file(apiServerOutput)
        doLast {
            lintDir("$projectDir/mockserver", mockserverOutput)
            lintDir("$projectDir/api-server", apiServerOutput)
        }
    }

    val createCluster by registering {
        description = "Create a kind cluster named vauhtijuoksu"
        group = "Application"
        doLast {
            val res = exec {
                bashCommand("kind get clusters | grep ^vauhtijuoksu$")
                standardOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            if (res.exitValue == 0) {
                logger.info("""Cluster named "vauhtijuoksu" already exists""")
            } else {
                exec {
                    workingDir = File("$projectDir")
                    bashCommand("kind create cluster --name vauhtijuoksu --config kind-cluster-config.yaml")
                }
                exec {
                    workingDir = File("$projectDir")
                    bashCommand("kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml")
                }
                exec {
                    workingDir = File("$projectDir")
                    // Sleep a bit because the resource is not yet created
                    bashCommand("sleep 15 && kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s")
                }
            }
        }
    }

    val runInCluster by registering {
        description = "Create a kind cluster and install vauhtijuoksu in it"
        group = "Application"
        dependsOn(dockerImage, createCluster)
        doLast {
            exec {
                bashCommand("kind load docker-image vauhtijuoksu/vauhtijuoksu-api:dev --name vauhtijuoksu")
            }
            exec {
                workingDir = File("$projectDir")
                bashCommand("helm upgrade --install vauhtijuoksu-api api-server --set image.registry=\"\"")
            }
        }
    }

    register("integrationTest") {
        description = "Run integration tests against a local kind cluster"
        group = "Verification"
        dependsOn(runInCluster)
        doLast {
            val output = FileOutputStream("$buildDir/curl-output.txt")
            exec {
                bashCommand("curl -vf --retry 10 --retry-all-errors localhost/gamedata")
                standardOutput = output
                errorOutput = output
            }
        }
    }

    val tearDownCluster by registering {
        description = "Tear down a kind cluster named vauhtijuoksu"
        group = "Application"
        doLast {
            exec {
                workingDir = File("$projectDir")
                bashCommand("kind delete cluster --name vauhtijuoksu")
            }
        }
    }

    check {
        finalizedBy(lintCharts)
    }

    clean {
        finalizedBy(tearDownCluster)
    }
}
