import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_AFFECTED_ENV_KEY
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_AFFECTED_KEY
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_MODE_ENV_KEY
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_MODE_KEY
import org.jetbrains.kotlin.systemTest.SystemTestMode

if (project.isSystemTestFederationEnabled.orNull == true) {
    val systemTestRuntime = configurations.detachedConfiguration(dependencies.project(":repo:system-tests")).apply {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

    tasks.withType<Test>().configureEach {
        val currentTestSystem = project.testSystem
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
        }
    }

    afterEvaluate {
        tasks.withType<Test>().configureEach {
            classpath += systemTestRuntime.incoming.files

            /*
            When running in smoke test mode, a given test task might actually not provide any smoke test
            */
            failOnNoDiscoveredTests.value(systemTestMode.map { it != SystemTestMode.Smoke })
        }
    }
}
