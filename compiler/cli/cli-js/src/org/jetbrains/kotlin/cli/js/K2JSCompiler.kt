/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.CliDiagnostics.WEB_ARGUMENT_WARNING
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyK2JSCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.web.*
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform

class K2JSCompiler : KotlinJsCompilerBase<K2JSCompilerArguments>() {
    override val builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    override fun createCliPipeline(arguments: K2JSCompilerArguments): WebCliPipeline<K2JSCompilerArguments> {
        return if (arguments.wasm) {
            LegacyJsWasmPipelineAdapter(WasmCliPipeline(defaultPerformanceManager))
        } else {
            JsCliPipeline(defaultPerformanceManager)
        }
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services
    ) {
        CommonJsConfigurationUpdater.setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
    }

    override fun initializeCommonConfiguration(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        rootDisposable: Disposable,
    ) {
        CommonJsConfigurationUpdater.initializeCommonConfiguration(configuration, arguments, rootDisposable)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JSCompiler(), args)
        }
    }
}

internal fun K2JSCompilerArguments.toWasmArguments(diagnosticsCollector: CompilerConfiguration): KotlinWasmCompilerArguments {
    diagnosticsCollector.report(
        WEB_ARGUMENT_WARNING,
        "Use `KotlinWasmCompiler` when compiling to Wasm. Using Wasm related arguments with `K2JSCompiler` will become an error in a future compiler version."
    )
    return copyK2JSCompilerArguments(this, KotlinWasmCompilerArguments())
}

fun RuntimeDiagnostic.Companion.resolve(
    value: String?,
    configuration: CompilerConfiguration
): RuntimeDiagnostic? = when (value?.lowercase()) {
    RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
    RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
    null -> null
    else -> {
        configuration.report(WEB_ARGUMENT_WARNING, "Unknown runtime diagnostic '$value'")
        null
    }
}
