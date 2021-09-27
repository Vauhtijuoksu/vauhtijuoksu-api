plugins {
    // Base plugin provides default lifecycle tasks and their dependency order
    base
    id("vauhtijuoksu-api.kotlin-common-conventions")
    id("org.hidetake.swagger.generator")
}

dependencies {
    swaggerUI("org.webjars:swagger-ui:3.10.0")
}

tasks {
    val swaggerInput = file("$projectDir/openapi.yaml")
    validateSwagger {
        swaggerSources {
            inputFile = swaggerInput
        }
    }

    generateSwaggerUI {
        swaggerSources {
            inputFile = swaggerInput
        }
    }

    check {
        dependsOn(validateSwagger)
    }

    build {
        dependsOn(generateSwaggerUI)
    }
}
