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

    @MavenTest
    fun `test-smart-defaults-execution-level-source-dirs-override-smart-defaults`(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-execution-source-dirs", mavenVersion, buildOptions) {
            build("compile", "test-compile") {
                assertBuildLogContains("Kotlin smart defaults are enabled")

                // compile should use only explicitly specified execution-level sourceDirs if present
                assertFileExists("target/classes/sample/CustomMain.class")
                assertFileDoesNotExist("target/classes/sample/DefaultMain.class") {
                    "Default main source root was compiled unexpectedly"
                }

                // test-compile should use only explicitly specified execution-level sourceDirs if present
                assertFileExists("target/test-classes/sample/CustomTest.class")
                assertFileDoesNotExist("target/test-classes/sample/DefaultTest.class") {
                    "Default test source root was compiled unexpectedly"
                }
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-empty-execution-level-source-dirs-fallback-to-smart-defaults`(
        mavenVersion: TestVersions.Maven,
    ) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-empty-execution-source-dirs", mavenVersion, buildOptions) {
            build("compile", "test-compile") {
                assertBuildLogContains("Kotlin smart defaults are enabled")

                // empty explicit execution-level sourceDirs should be treated as absent
                assertFileExists("target/classes/sample/DefaultMain.class")
                assertFileExists("target/test-classes/sample/DefaultTest.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-execution-level-source-dirs-do-not-produce-duplicate-source-root-warnings`(
        mavenVersion: TestVersions.Maven,
    ) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-execution-source-dirs-no-duplicates", mavenVersion, buildOptions) {
            build("compile", "test-compile") {
                assertBuildLogContains("Kotlin smart defaults are enabled")
                assertBuildLogDoesNotContain("Duplicate source root")

                // compile should include all explicitly configured source directories
                assertFileExists("target/classes/sample/CustomMain.class")
                assertFileExists("target/classes/sample/DefaultMain.class")

                // test-compile should include all explicitly configured test source directories
                assertFileExists("target/test-classes/sample/CustomTest.class")
                assertFileExists("target/test-classes/sample/DefaultTest.class")
            }
        }
    }
}
