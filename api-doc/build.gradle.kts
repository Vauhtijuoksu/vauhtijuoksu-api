plugins {
    // Base plugin provides default lifecycle tasks and their dependency order
    base
    id("vauhtijuoksu-api.kotlin-common-conventions")
    id("org.hidetake.swagger.generator")
    id("org.openapi.generator") version "5.1.1"
}

dependencies {
    swaggerCodegen("io.swagger.codegen.v3:swagger-codegen-cli:3.0.26")
    swaggerUI("org.webjars:swagger-ui:3.10.0")
    implementation("org.openapi.generator:5.2.0")
}

tasks {
    validateSwagger {
        swaggerSources {
            inputFile = file("$projectDir/openapi.yaml")
        }
    }

    openApiGenerate {
        inputSpec.set("$projectDir/openapi.yaml")
        generatorName.set("kotlin-vertx")
    }

    generateSwaggerUI {
        swaggerSources {
            inputFile = file("$projectDir/openapi.yaml")
        }
    }

    check {
        dependsOn(validateSwagger)
    }

    compileKotlin {
        dependsOn(openApiGenerate)
    }

    build {
        dependsOn(generateSwaggerUI)
    }
}
