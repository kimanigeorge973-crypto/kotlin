import org.gradle.api.tasks.testing.Test
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN
import org.jetbrains.kotlin.systemTest.SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Test.withSmokeTestPattern(@Language("RegExp") pattern: String) {
    systemProperty(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN, pattern)
    environment(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY, pattern)
}
