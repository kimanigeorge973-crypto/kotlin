import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager

description = "Kotlin Power-Assert Compiler Plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val junit5Classpath by configurations.creating
val powerAssertJvmRuntimeClasspath: Configuration by configurations.creating
val powerAssertJsRuntimeClasspath: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}
val powerAssertNativeRuntimeClasspath: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, HostManager.host.name)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

dependencies {
    embedded(project(":kotlin-power-assert-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-power-assert-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-power-assert-compiler-plugin.backend"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    junit5Classpath(libs.junit.jupiter.api)
    powerAssertJvmRuntimeClasspath(project(":kotlin-power-assert-runtime")) { isTransitive = false }
    powerAssertJsRuntimeClasspath(project(":kotlin-power-assert-runtime")) { isTransitive = false }
    powerAssertNativeRuntimeClasspath(project(":kotlin-power-assert-runtime")) { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        addClasspathProperty(junit5Classpath, "junit5.classpath")
        addClasspathProperty(powerAssertJvmRuntimeClasspath, "powerAssertRuntime.jvm.classpath")
        addClasspathProperty(powerAssertJsRuntimeClasspath, "powerAssertRuntime.js.classpath")
    }

    testGenerator("org.jetbrains.kotlin.powerassert.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    testData(project.isolated, "testData")
}
