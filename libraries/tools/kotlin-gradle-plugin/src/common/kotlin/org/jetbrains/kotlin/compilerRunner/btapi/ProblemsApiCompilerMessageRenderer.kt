/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporter
import java.io.File

internal class ProblemsApiCompilerMessageRenderer(
    private val problemsReporter: CompilerDiagnosticsProblemsReporter,
) : CompilerMessageRenderer {
    override fun render(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
    ): String {
        problemsReporter.reportCompilerMessage(severity, message, location)

        return buildString {
            location?.apply {
                val fileUri = File(path).toPath().toUri()
                append(fileUri)
                if (line > 0 && column > 0) {
                    append(":$line:$column")
                }
                append(' ')
            }
            append(message)
        }
    }
}
