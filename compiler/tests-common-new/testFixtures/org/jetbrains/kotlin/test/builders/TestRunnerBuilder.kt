/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.test.FirstPhaseTestRunner

inline fun firstPhaseTestRunner(testDataPath: String, crossinline init: TestConfigurationBuilder.() -> Unit): FirstPhaseTestRunner {
    return FirstPhaseTestRunner(testConfiguration(testDataPath, init))
}
