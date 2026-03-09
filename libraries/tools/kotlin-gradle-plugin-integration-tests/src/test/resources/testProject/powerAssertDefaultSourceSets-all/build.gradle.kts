plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
}

dependencies {
    testImplementation(kotlin("test"))
}

powerAssert {
    functions.addAll("kotlin.require")
    defaultSourceSets.set(org.jetbrains.kotlin.powerassert.gradle.DefaultSourceSets.ALL)
}
