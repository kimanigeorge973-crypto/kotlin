/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.io.path.Path

class JvmCompilationOperationDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = kotlinToolchains.jvm.jvmCompilationOperationBuilder(emptyList(), Path(".")).build()
        // access does not throw any exception on access, meaning there's some default value, the actual value is checked in BuildOperationDefaultsTest
        assertDoesNotThrow { operation[BuildOperation.METRICS_COLLECTOR] }
        assertEquals(null, operation[JvmCompilationOperation.INCREMENTAL_COMPILATION])
        assertEquals(null, operation[JvmCompilationOperation.LOOKUP_TRACKER])
        assertEquals(null, operation[JvmCompilationOperation.KOTLINSCRIPT_EXTENSIONS])
        assertEquals(CompilerArgumentsLogLevel.DEBUG, operation[JvmCompilationOperation.COMPILER_ARGUMENTS_LOG_LEVEL])
        assertEquals(false, operation[JvmCompilationOperation.GENERATE_COMPILER_REF_INDEX])
        assertNotNull(operation[JvmCompilationOperation.COMPILER_MESSAGE_RENDERER])
    }
}