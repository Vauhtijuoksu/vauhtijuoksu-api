import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
    id("org.hidetake.swagger.generator")
    alias(libs.plugins.openapi.generator)
}

dependencies {
    swaggerUI(libs.swagger.ui)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jackson.annotations)
}

sourceSets {
    main {
        kotlin.srcDir("${layout.buildDirectory.get()}/generated-models/src/main/kotlin")
    }
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

    val generateClient by registering(GenerateTask::class) {
        generatorName.set("kotlin")
        inputSpec.set("${layout.projectDirectory}/openapi.yaml")
        outputDir.set("${layout.buildDirectory.get()}/generated-client")
        modelPackage.set("apimodels")
        cleanupOutput.set(true)
        configOptions.putAll(
            mapOf(
                "serializationLibrary" to "jackson",
                "dateLibrary" to "kotlinx-datetime",
                "companionObject" to "true",
            ),
        )
    }

    register<Copy>("copy-models") {
        dependsOn(generateClient)
        delete("${layout.buildDirectory.get()}/generated-models")
        from("${layout.buildDirectory.get()}/generated-client/src/main/kotlin/apimodels")
        into("${layout.buildDirectory.get()}/generated-models/src/main/kotlin/apimodels")
    }

    check {
        dependsOn(validateSwagger)
    }

    compileKotlin {
        dependsOn("copy-models")
    }

    build {
        dependsOn(generateSwaggerUI)
    }
}
