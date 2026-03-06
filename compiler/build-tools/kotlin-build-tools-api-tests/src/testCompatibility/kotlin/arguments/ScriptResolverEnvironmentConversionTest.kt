/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class ScriptResolverEnvironmentConversionTest : BaseArgumentTest<Map<String, String>>("Xscript-resolver-environment") {

    @DisplayName("ScriptResolverEnvironment is converted to '-Xscript-resolver-environment' argument")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentToArgumentString(toolchain: KotlinToolchains) {
        val environment = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = environment
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(environment), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xscript-resolver-environment' has the default value when ScriptResolverEnvironment is not set")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ScriptResolverEnvironment can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentGetWhenSet(toolchain: KotlinToolchains) {
        val expectedEnvironment = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = expectedEnvironment
        }.build()

        val actualEnvironment = jvmOperation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]

        assertEquals(expectedEnvironment, actualEnvironment)
    }

    @DisplayName("ScriptResolverEnvironment has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val environment = jvmOperation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(environment)
        )
    }

    @DisplayName("Raw argument strings '-Xscript-resolver-environment=<value>' are converted to ScriptResolverEnvironment")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsScriptResolverEnvironmentConversion(toolchain: KotlinToolchains) {
        val expectedEnvironment = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedEnvironment),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedEnvironment,
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsScriptResolverEnvironment(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT])
        )
    }

    @DisplayName("Raw argument with non-existent ScriptResolverEnvironment value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidAssertionsModeConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value", toolchain.getCompilerVersion())
            )
        }

        assertEquals("Invalid -$argumentName value format: non-existent-value", exception.message)
    }

    override fun expectedArgumentStringsFor(value: String?, compilerVersion: String): List<String> {
        if (value == null || value == getDefaultValueString(compilerVersion)) {
            return emptyList()
        }

        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Map<String, String>?): String? = argument?.entries?.joinToString(",") { "${it.key}=${it.value}" }
}
