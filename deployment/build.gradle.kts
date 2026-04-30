import fi.vauhtijuoksu.utilities.ExecOperationsProvider
import fi.vauhtijuoksu.utilities.bashCommand
import java.io.FileOutputStream

plugins {
    id("vauhtijuoksu-api.common-conventions")
}

val dockerImage: Configuration by configurations.creating {
}

dependencies {
    dockerImage(project(path = ":server", "dockerImageConfiguration"))
}

val execOperations = objects.newInstance(ExecOperationsProvider::class).execOperations

tasks {
    val lintCharts by registering {
        fun lintDir(chartDirectory: String, output: String) {
            val res = execOperations.exec {
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
            val res = execOperations.exec {
                bashCommand("kind get clusters | grep ^vauhtijuoksu$")
                isIgnoreExitValue = true
            }
            if (res.exitValue == 0) {
                logger.info("""Cluster named "vauhtijuoksu" already exists""")
                // Ensure that local cluster is used for following commands, and not for example production cluster
                execOperations.exec {
                    bashCommand("kind export kubeconfig --name vauhtijuoksu")
                }
            } else {
                execOperations.exec {
                    workingDir = projectDir
                    bashCommand("kind create cluster --name vauhtijuoksu --config kind-cluster/kind-cluster-config.yaml")
                }
                execOperations.exec {
                    workingDir = projectDir
                    bashCommand(
                        """
                        helm repo add traefik https://traefik.github.io/charts
                        helm repo update
                        helm install traefik traefik/traefik --namespace traefik --create-namespace -f kind-cluster/traefik-values.yaml
                        """,
                    )
                }
                // Utilize the wait time that would otherwise be spent waiting on ingress by installing postgres here
                execOperations.exec {
                    bashCommand(
                        """
                        helm repo add bitnami https://charts.bitnami.com/bitnami
                        helm repo update
                        # Download postgres image to docker on host so it's not downloaded for each new cluster
                        # Upgrade version for image tag in deployment/kind-cluster/psql-values.yaml when upgrading this
                        docker image pull postgres:10.20
                        kind load docker-image postgres:10.20 --name vauhtijuoksu
                        """,
                    )
                }
                execOperations.exec {
                    workingDir = projectDir
                    bashCommand("helm install postgres bitnami/postgresql -f kind-cluster/psql-values.yaml --version 12.1.5")
                }
                // Postgres secrets for vauhtijuoksu api. Created manually on production environment
                execOperations.exec {
                    workingDir = projectDir
                    bashCommand("kubectl create secret generic vauhtijuoksu-api-psql --from-file kind-cluster/psql-secret.yaml")
                }
                execOperations.exec {
                    workingDir = projectDir
                    bashCommand("kubectl create secret generic vauhtijuoksu-api-htpasswd --from-file kind-cluster/htpasswd")
                }
                // Install valkey
                execOperations.exec {
                    bashCommand(
                        """
                        kubectl create secret generic --from-literal=REDIS__PASSWORD="not used" vauhtijuoksu-api-redis
                        helm repo add valkey https://valkey.io/valkey-helm/
                        helm repo update
                        helm install valkey valkey/valkey
                        """.trimIndent(),
                    )
                }
                // Install OAuth secret. Fill in the values in the secret if you wish to try OAuth
                val secret = File("$projectDir/kind-cluster/oauth-secret.yaml")
                val secretTemplate = File("$projectDir/kind-cluster/oauth-secret-template.yaml")
                val oAuthSecret = if (secret.exists()) secret else secretTemplate
                execOperations.exec {
                    bashCommand("kubectl create secret generic vauhtijuoksu-api-oauth --from-file ${oAuthSecret.path}")
                }

                // Wait for traefik
                execOperations.exec {
                    workingDir = projectDir
                    bashCommand("kubectl rollout status deployment traefik -n traefik --timeout=90s")
                }
            }
        }
    }

    val runInCluster by registering {
        description = "Create a kind cluster and install vauhtijuoksu in it"
        group = "Application"
        dependsOn(dockerImage, createCluster)
        doLast {
            execOperations.exec {
                bashCommand(
                    """
                    docker tag vauhtijuoksu/vauhtijuoksu-api:${rootProject.version} localhost/vauhtijuoksu/vauhtijuoksu-api:${rootProject.version}
                    kind load docker-image localhost/vauhtijuoksu/vauhtijuoksu-api:${rootProject.version} --name vauhtijuoksu
                    """.trimIndent(),
                )
            }
            execOperations.exec {
                workingDir = projectDir
                bashCommand(
                    """
                helm upgrade --install vauhtijuoksu-api api-server -f kind-cluster/vauhtijuoksu-api-values.yaml --set image.tag=${rootProject.version}
                # Force restart, because in development pods might have a same dirty version if no commits were made
                kubectl delete pod -l app.kubernetes.io/name=vauhtijuoksu-api
                kubectl rollout status deployment vauhtijuoksu-api
                """,
                )
            }
        }
    }

    register("localMockApi") {
        description = "Create a kind cluster and serve mock api"
        group = "Application"
        dependsOn(createCluster)
        doLast {
            execOperations.exec {
                workingDir = projectDir
                bashCommand("helm upgrade --install mockserver mockserver")
            }
        }
    }

    register("integrationTest") {
        description = "Run integration tests against a local kind cluster"
        group = "Verification"
        dependsOn(runInCluster)
        doLast {
            val output = FileOutputStream("$buildDir/curl-output.txt")
            execOperations.exec {
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
            execOperations.exec {
                workingDir = projectDir
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
