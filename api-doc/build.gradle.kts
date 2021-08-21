plugins {
    // Base plugin provides default lifecycle tasks and their dependency order
    base
    id("vauhtijuoksu-api.kotlin-common-conventions")
    id("org.hidetake.swagger.generator")
}

dependencies {
    swaggerCodegen("io.swagger.codegen.v3:swagger-codegen-cli:3.0.26")
    swaggerUI("org.webjars:swagger-ui:3.10.0")
}

tasks {
    validateSwagger {
        swaggerSources {
            inputFile = file("$projectDir/openapi.yaml")
        }
    }

    generateSwaggerUI {
        swaggerSources {
            inputFile = file("$projectDir/openapi.yaml")
        }
    }

    check {
        dependsOn(validateSwagger)
    }

    build {
        dependsOn(generateSwaggerUI)
    }
}
