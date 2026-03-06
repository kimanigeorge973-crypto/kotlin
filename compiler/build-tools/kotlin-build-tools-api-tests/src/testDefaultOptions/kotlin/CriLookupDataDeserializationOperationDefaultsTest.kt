/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.cri
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class CriLookupDataDeserializationOperationDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = kotlinToolchains.cri.createCriLookupDataDeserializationOperation(byteArrayOf())
        // access does not throw any exception on access, meaning there's some default value, the actual value is checked in BuildOperationDefaultsTest
        assertDoesNotThrow { operation[BuildOperation.METRICS_COLLECTOR] }
    }
}