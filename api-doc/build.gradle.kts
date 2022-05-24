plugins {
    id("vauhtijuoksu-api.common-conventions")
    id("org.hidetake.swagger.generator")
}

dependencies {
    swaggerUI("org.webjars:swagger-ui:4.11.1")
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
