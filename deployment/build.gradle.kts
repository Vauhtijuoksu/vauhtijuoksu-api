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
                isIgnoreExitValue = true
            }
            if (res.exitValue == 0) {
                logger.info("""Cluster named "vauhtijuoksu" already exists""")
                // Ensure that local cluster is used for following commands, and not for example production cluster
                exec {
                    bashCommand("kind export kubeconfig --name vauhtijuoksu")
                }
            } else {
                exec {
                    workingDir = projectDir
                    bashCommand("kind create cluster --name vauhtijuoksu --config kind-cluster/kind-cluster-config.yaml")
                }
                exec {
                    workingDir = projectDir
                    bashCommand(
                        """
                        docker pull k8s.gcr.io/ingress-nginx/controller:v1.0.4@sha256:545cff00370f28363dad31e3b59a94ba377854d3a11f18988f5f9e56841ef9ef
                        docker pull k8s.gcr.io/ingress-nginx/kube-webhook-certgen:v1.1.1@sha256:64d8c73dca984af206adf9d6d7e46aa550362b1d7a01f3a0a91b20cc67868660
                        kind load docker-image k8s.gcr.io/ingress-nginx/controller:v1.0.4@sha256:545cff00370f28363dad31e3b59a94ba377854d3a11f18988f5f9e56841ef9ef --name vauhtijuoksu
                        kind load docker-image k8s.gcr.io/ingress-nginx/kube-webhook-certgen:v1.1.1@sha256:64d8c73dca984af206adf9d6d7e46aa550362b1d7a01f3a0a91b20cc67868660 --name vauhtijuoksu
                        kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/14f6b32032b709d3e0f614ca85954c3583c5fe3d/deploy/static/provider/kind/deploy.yaml
                        """
                    )
                }
                // Utilize the wait time that would otherwise be spent waiting on ingress by installing postgres here
                exec {
                    bashCommand(
                        """
                        helm repo add bitnami https://charts.bitnami.com/bitnami
                        helm repo update
                        # Download postgres image to docker on host so it's not downloaded for each new cluster
                        # Upgrade version for image tag in deployment/kind-cluster/psql-values.yaml when upgrading this
                        docker image pull bitnami/postgresql:10.18.0-debian-10-r60
                        kind load docker-image bitnami/postgresql:10.18.0-debian-10-r60 --name vauhtijuoksu
                        """
                    )
                }
                exec {
                    workingDir = projectDir
                    bashCommand("helm install postgres bitnami/postgresql -f kind-cluster/psql-values.yaml --version ^10.15.1")
                }
                // Postgres secrets for vauhtijuoksu api. Created manually on production environment
                exec {
                    workingDir = projectDir
                    bashCommand("kubectl create secret generic vauhtijuoksu-api-psql --from-file kind-cluster/secret-conf.yaml")
                }
                exec {
                    workingDir = projectDir
                    bashCommand("kubectl create secret generic vauhtijuoksu-api-htpasswd --from-file kind-cluster/htpasswd")
                }
                // Wait for ingress
                exec {
                    workingDir = projectDir
                    bashCommand("kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s")
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
                bashCommand("kind load docker-image vauhtijuoksu/vauhtijuoksu-api:${rootProject.version} --name vauhtijuoksu")
            }
            exec {
                workingDir = projectDir
                bashCommand(
                    """
                helm upgrade --install vauhtijuoksu-api api-server -f kind-cluster/vauhtijuoksu-api-values.yaml --set image.tag=${rootProject.version}
                # Force restart, because in development pods might have a same dirty version if no commits were made
                kubectl delete pod -l app=vauhtijuoksu-api
                kubectl rollout status deployment vauhtijuoksu-api
                """
                )
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
