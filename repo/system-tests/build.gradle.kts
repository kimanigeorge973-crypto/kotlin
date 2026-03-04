import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
}

kotlin.target.compilations.all {
    compileTaskProvider.configure {
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_1)
            apiVersion.set(KotlinVersion.KOTLIN_2_1)
        }
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.junit.jupiter.api)
}
