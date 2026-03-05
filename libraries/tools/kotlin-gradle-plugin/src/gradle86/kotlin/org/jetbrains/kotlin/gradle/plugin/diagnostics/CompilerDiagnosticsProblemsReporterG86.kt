/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class CompilerDiagnosticsProblemsReporterG86 @Inject constructor(
    private val problems: Problems,
) : CompilerDiagnosticsProblemsReporter {
    private val logger: Logger by lazy { Logging.getLogger(this.javaClass) }
    private val reporter by lazy { problems.forNamespace("org.jetbrains.kotlin.gradle.plugin") }

    override fun reportCompilerMessage(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
    ) {
        val gradleSeverity = severity.toGradleSeverity() ?: return

        try {
            reporter.reporting {
                it
                    .category(severity.toDiagnosticGroup().groupId)
                    .label(severity.toDisplayName())
                    .details(message)
                    .severity(gradleSeverity)
                    .applySourceLocation(location)
            }
        } catch (e: NoSuchMethodError) {
            logger.error("Can't invoke reporter method:", e)
        }
    }

    class Factory : CompilerDiagnosticsProblemsReporter.Factory {
        override fun getInstance(objects: ObjectFactory): CompilerDiagnosticsProblemsReporter {
            return objects.newInstance<CompilerDiagnosticsProblemsReporterG86>()
        }
    }
}

private fun CompilerMessageRenderer.Severity.toDiagnosticGroup(): DiagnosticGroup = when (this) {
    CompilerMessageRenderer.Severity.ERROR -> DiagnosticGroup.Compiler.Error
    CompilerMessageRenderer.Severity.WARNING -> DiagnosticGroup.Compiler.Warning
    CompilerMessageRenderer.Severity.INFO, CompilerMessageRenderer.Severity.DEBUG -> DiagnosticGroup.Compiler.Default
}

private fun CompilerMessageRenderer.Severity.toDisplayName(): String = when (this) {
    CompilerMessageRenderer.Severity.ERROR -> "Kotlin compiler error"
    CompilerMessageRenderer.Severity.WARNING -> "Kotlin compiler warning"
    CompilerMessageRenderer.Severity.INFO -> "Kotlin compiler info"
    CompilerMessageRenderer.Severity.DEBUG -> "Kotlin compiler debug"
}

private fun CompilerMessageRenderer.Severity.toGradleSeverity(): Severity? = when (this) {
    CompilerMessageRenderer.Severity.ERROR -> Severity.ERROR
    CompilerMessageRenderer.Severity.WARNING -> Severity.WARNING
    CompilerMessageRenderer.Severity.INFO, CompilerMessageRenderer.Severity.DEBUG -> null
}

private fun ProblemSpec.applySourceLocation(location: CompilerMessageRenderer.SourceLocation?): ProblemSpec {
    if (location == null) return this
    val path = location.path
    val line = location.line
    val column = location.column

    return when {
        line > 0 && column > 0 -> lineInFileLocation(path, line, column, location.locationLength)
        line > 0 -> lineInFileLocation(path, line)
        else -> fileLocation(path)
    }
}

private val CompilerMessageRenderer.SourceLocation.locationLength: Int
    get() {
        val length = if (line == lineEnd && columnEnd > column) {
            columnEnd - column
        } else {
            1
        }
        return length.coerceAtLeast(1)
    }
