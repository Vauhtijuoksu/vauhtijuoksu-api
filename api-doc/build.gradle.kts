plugins {
    id("vauhtijuoksu-api.common-conventions")
    id("org.hidetake.swagger.generator")
}

dependencies {
    swaggerUI(libs.swagger.ui)
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
            options["docExpansion"] = "none"
        }
    }

    check {
        dependsOn(validateSwagger)
    }

    build {
        dependsOn(generateSwaggerUI)
    }
}
