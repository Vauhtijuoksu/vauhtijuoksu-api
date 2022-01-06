/**
 * Configuration for projects including testable production code
 */
plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
    jacoco
}

tasks {
    jacocoTestReport {
        dependsOn(test)
        reports {
            csv.required.set(true)
            html.required.set(true)
        }
    }

    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }
}
