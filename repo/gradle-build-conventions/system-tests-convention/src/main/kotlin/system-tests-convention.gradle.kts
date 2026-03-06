import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.systemTest.*
import org.jetbrains.kotlin.systemTest.gradle.SystemTestExtension
import org.jetbrains.kotlin.systemTest.gradle.affectedTestSystemsService
import org.jetbrains.kotlin.systemTest.gradle.testSystem

val extension = extensions.create<SystemTestExtension>("systemTests")

if (project.isSystemTestFederationEnabled.orNull == true) {
    val systemTestRuntime = configurations.detachedConfiguration(dependencies.project(":repo:system-tests")).apply {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

    tasks.withType<Test>().configureEach {
        val currentTestSystem = project.provider { TestSystem.Unknown } // project.testSystem
        val systemTestMode = project.systemTestMode
        val affectedTestSystems = project.affectedTestSystems

        doFirst {
            logger.quiet("Current Test System: '$currentTestSystem'")
            logger.quiet("System Test Mode: '${systemTestMode.get()}'")
            systemProperty(SYSTEM_TEST_MODE_KEY, systemTestMode.get().name)
            environment(SYSTEM_TEST_MODE_ENV_KEY, systemTestMode.get().name)

            val formattedAffectedTestSystems = affectedTestSystems.get().joinToString(separator = ";") { it.name }
            logger.quiet("Affected Test Systems: '$formattedAffectedTestSystems'")
            systemProperty(SYSTEM_TEST_AFFECTED_KEY, formattedAffectedTestSystems)
            environment(SYSTEM_TEST_AFFECTED_ENV_KEY, formattedAffectedTestSystems)
            systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

            if (systemTestMode.get() == SystemTestMode.Smoke) {
                val testFramework = testFramework
                if (testFramework is JUnitPlatformTestFramework) {
                    testFramework.options.includeTags("smoke")
                    affectedTestSystems.get().forEach { testSystem ->
                        testFramework.options.includeTags("contract:${testSystem.name}")
                    }
                }

                if (testFramework is JUnitTestFramework) {
                    testFramework.options.includeCategories("org.jetbrains.kotlin.systemTest.SmokeTest")
                }

                println("##teamcity[addBuildTag 'System Test Mode: Smoke']")
                affectedTestSystems.get().forEach { testSystem ->
                    println("##teamcity[addBuildTag 'Affected: $testSystem']")
                }
            }
        }
    }

    afterEvaluate {
        tasks.withType<Test>().configureEach {
            classpath += systemTestRuntime.incoming.files

            /*
            When running in smoke test mode, a given test task might actually not provide any smoke test
            */

            //doFirst {
            //   failOnNoDiscoveredTests.value(systemTestMode.map { it != SystemTestMode.Smoke })
            // }
        }
    }
}

afterEvaluate {
    if (extension.defaultDependencyEnabled.get()) {
        dependencies {
            configurations.findByName("testImplementation")?.name(project(":repo:system-tests"))
            configurations.findByName("jvmTestImplementation")?.name(project(":repo:system-tests"))
        }
    }
}
