/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SmartDefaultsIT : KotlinMavenTestBase() {

    @MavenTest
    fun `test-smart-defaults-kapt`(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-kapt", mavenVersion, buildOptions) {
            build("verify") {
                // Build succeeded and JAR was produced
                assertFileExists("app/target/app-1.0-SNAPSHOT.jar") { "App JAR was not produced" }

                // KAPT ran and generated a Java source file from @Anno on KotlinService
                assertFileExists(
                    "app/target/generated-sources/kapt/compile/app/KotlinServiceGenerated.java"
                ) { "KAPT-generated Java source file was not found" }

                // KAPT ran and generated a Kotlin extension file from @Anno on KotlinService
                assertFileExists(
                    "app/target/generated-sources/kaptKotlin/compile/KotlinServiceExtensions.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }

                // Tests successfully ran and produced Surefire XML reports
                assertFileExists(
                    "app/target/surefire-reports/TEST-app.KotlinServiceTest.xml"
                ) { "Surefire report for KotlinServiceTest not found" }
                assertFileExists(
                    "app/target/surefire-reports/TEST-app.JavaConsumerTest.xml"
                ) { "Surefire report for JavaConsumerTest not found" }

                assertBuildLogContains(
                    // 2 tests in the `KotlinServiceTest`
                    "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0",
                    // 3 tests in the `JavaConsumerTest`
                    "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0",
                    "BUILD SUCCESS",
                )
            }
        }
    }
}
