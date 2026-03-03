import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY
import org.jetbrains.kotlin.systemTest.SystemTestMode
import org.jetbrains.kotlin.systemTest.TestSystem

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Test.withSmokeTestPattern(@Language("RegExp") pattern: String) {
    systemProperty(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN, pattern)
    environment(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY, pattern)
}

val Project.systemTestMode: Provider<SystemTestMode>
    get() = project.project.providers.of(SystemTestModeValueSource::class.java) {
        parameters.testSystem.set(project.testSystem)
        parameters.service.set(project.affectedSystemBuildService)
    }


val Project.affectedTestSystems: Provider<Set<TestSystem>>
    get() = project.providers.of(AffectedTestSystemValueSource::class.java) {
        parameters.service.set(project.affectedSystemBuildService)
    }
