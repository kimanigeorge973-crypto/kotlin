/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.web.WebCliPipeline
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.utils.KotlinPaths

abstract class KotlinJsCompilerBase<T : CommonJsAndWasmCompilerArguments> : CLICompiler<T>() {
    abstract val builtInsPlatform: BuiltInsPlatform
    override val platform: TargetPlatform = JsPlatforms.defaultJsPlatform

    override fun doExecutePhased(
        arguments: T,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode? {
        return createCliPipeline(arguments).execute(arguments, services, basicMessageCollector)
    }

    abstract fun createCliPipeline(arguments: T): WebCliPipeline<T>

    override fun doExecute(
        arguments: T,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode = error("K1 compiler entry point is no longer supported.")

    abstract fun initializeCommonConfiguration(configuration: CompilerConfiguration, arguments: T, rootDisposable: Disposable)

    override fun executableScriptFileName(): String = "kotlinc-js"

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: T) {}

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JSCompiler(), args)
        }
    }
}