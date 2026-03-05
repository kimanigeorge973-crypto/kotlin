/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("unused")

package org.jetbrains.kotlin.systemTest

const val SYSTEM_TEST_FEDERATION_ENABLED_KEY = "system.test.federation"
const val SYSTEM_TEST_FEDERATION_ENABLED_ENV_KEY = "SYSTEM_TEST_FEDERATION"

val currentSystemTestFederationEnabled: Boolean = run {
    val raw = System.getenv(SYSTEM_TEST_FEDERATION_ENABLED_ENV_KEY) ?: System.getProperty(SYSTEM_TEST_FEDERATION_ENABLED_KEY) ?: return@run false
    return@run raw.toBoolean()
}

const val SYSTEM_TEST_MODE_KEY = "system.test.mode"
const val SYSTEM_TEST_MODE_ENV_KEY = "SYSTEM_TEST_MODE"

val currentSystemTestModeOrNull: SystemTestMode? = run {
    val raw = System.getenv(SYSTEM_TEST_MODE_ENV_KEY) ?: System.getProperty(SYSTEM_TEST_MODE_KEY) ?: return@run null
    return@run SystemTestMode.valueOf(raw)
}

val currentSystemTestMode: SystemTestMode = run {
    currentSystemTestModeOrNull ?: SystemTestMode.Full
}

const val SYSTEM_TEST_AFFECTED_KEY = "system.test.affectedSystems"
const val SYSTEM_TEST_AFFECTED_ENV_KEY = "SYSTEM_TEST_AFFECTED_SYSTEMS"

val currentAffectedTestSystems: Set<TestSystem>? = run {
    val raw = System.getenv(SYSTEM_TEST_AFFECTED_ENV_KEY) ?: System.getProperty(SYSTEM_TEST_AFFECTED_KEY) ?: return@run null
    if (raw.isEmpty()) return@run emptySet()
    return@run raw.split(";").map { TestSystem.valueOf(it) }.toSet()
}


const val SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN = "system.test.smokeTestFqnPattern"
const val SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY = "SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN"

val currentSmokeTestPattern: Regex? = run {
    val raw = System.getenv(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY) ?: System.getProperty(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN)
    ?: return@run null
    return@run Regex(raw)
}

const val SYSTEM_TEST_DIFF_FILE_ENV_KEY = "TEST_SYSTEMS_DIFF_FILE"
