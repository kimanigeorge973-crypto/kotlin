/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

/**
 * Handles forward compatibility when the API version is older than the implementation version.
 * 
 * Converts between old API argument types (e.g., `String`) and new implementation types (e.g., `Path`)
 * to maintain compatibility when argument type definitions evolve between API and implementation.
 */
@OptIn(ExperimentalCompilerArgument::class)
internal interface CompilerArgumentValueAdapter<V> {

    fun <T> mapFrom(value: Any?, key: V): T?
    fun <T> mapTo(value: Any?, key: V): T?

    companion object {
        //TODO(KT-84598): Expose API Version via Public Property
        private val requiresPre240ForwardCompatibility: Boolean =
            JvmPlatformToolchain::class.functions.none { it.name == "discoverScriptExtensionsOperationBuilder" }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getOrNull(keyClass: KClass<T>): CompilerArgumentValueAdapter<T>? {
            return when (keyClass) {
                JvmCompilerArguments.JvmCompilerArgument::class if requiresPre240ForwardCompatibility -> {
                    JvmCompilerArgumentPre2_4_0ValueAdapter as CompilerArgumentValueAdapter<T>
                }
                else -> null
            }
        }
    }
}

@Suppress("ClassName")
@OptIn(ExperimentalCompilerArgument::class)
private object JvmCompilerArgumentPre2_4_0ValueAdapter : CompilerArgumentValueAdapter<JvmCompilerArguments.JvmCompilerArgument<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun <T> mapFrom(
        value: Any?,
        key: JvmCompilerArguments.JvmCompilerArgument<*>,
    ): T? {
        if (value == null) return null as T?
        return when (key) {
            JvmCompilerArguments.JDK_HOME -> {
                val pathValue = value as Path
                pathValue.absolutePathStringOrThrow() as T
            }

            JvmCompilerArguments.X_PROFILE -> {
                val profileCompilerCommand = value as ProfileCompilerCommand
                with(profileCompilerCommand) {
                    profilerPath.absolutePathStringOrThrow() +
                            "${File.pathSeparator}${command}" +
                            "${File.pathSeparator}" +
                            outputDir.absolutePathStringOrThrow()
                } as T
            }

            JvmCompilerArguments.X_ADD_MODULES -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as T
            }

            JvmCompilerArguments.CLASSPATH -> {
                val listValue = value as List<Path>
                listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as T
            }

            JvmCompilerArguments.X_KLIB -> {
                val listValue = value as List<Path>
                listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as T
            }

            JvmCompilerArguments.X_MODULE_PATH -> {
                val listValue = value as List<Path>
                listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as T
            }

            JvmCompilerArguments.X_FRIEND_PATHS -> {
                val listValue = value as List<Path>
                listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as T
            }

            JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                val listValue = value as List<Path>
                listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as T
            }

            JvmCompilerArguments.X_SCRIPT_RESOLVER_ENVIRONMENT -> {
                val mapValue: Map<String, String>? =
                    (value as? Map<*, *>)?.takeIf { it.all { entry -> entry.key is String && entry.value is String } } as Map<String, String>?
                mapValue?.entries?.map { "${it.key}=${it.value}" }?.toTypedArray() as T
            }

            else -> value as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> mapTo(
        value: Any?,
        key: JvmCompilerArguments.JvmCompilerArgument<*>,
    ): T? {
        if (value == null) return null as T?

        return when (key) {
            JvmCompilerArguments.JDK_HOME -> {
                val stringValue = value as String
                Path(stringValue) as T
            }

            JvmCompilerArguments.X_PROFILE -> {
                val stringValue = value as String
                val parts = stringValue.split(File.pathSeparator)
                require(parts.size == 3) { "Invalid async profiler settings format: $stringValue" }

                ProfileCompilerCommand(Path(parts[0]), parts[1], Path(parts[2])) as T
            }

            JvmCompilerArguments.X_ADD_MODULES -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            JvmCompilerArguments.CLASSPATH -> {
                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_KLIB -> {
                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_MODULE_PATH -> {
                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_FRIEND_PATHS -> {
                val arrayValue = value as Array<String>
                arrayValue.map { Path(it) } as T
            }

            JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                val arrayValue = value as Array<String>
                arrayValue.map { Path(it) } as T
            }

            JvmCompilerArguments.X_SCRIPT_RESOLVER_ENVIRONMENT -> {
                val arrayValue = value as Array<String>
                arrayValue.associate {
                    val parts = it.split("=", limit = 2)
                    if (parts.size != 2) {
                        throw CompilerArgumentsParseException("Invalid -Xscript-resolver-environment value format: $it")
                    }

                    Pair(parts[0], parts[1])
                } as T
            }

            else -> value as T
        }
    }
}