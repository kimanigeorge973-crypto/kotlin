package org.jetbrains.kotlin.systemTest.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.systemTest.TestSystem

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


internal val Project.testSystem: TestSystem
    get() {
        return when {
            this.path.contains("gradle") -> TestSystem.Gradle
            this.path.contains("compiler") -> TestSystem.Compiler
            this.path.contains("wasm") -> TestSystem.Compiler
            this.path.contains("js") -> TestSystem.Compiler
            this.path.contains("native") -> TestSystem.Compiler
            else -> TestSystem.Unknown
        }
    }


internal fun affectedTestSystems(changedFilePaths: List<String>): Set<TestSystem> {
    val affected = changedFilePaths.map { file ->
        if (file.contains("compiler/")) {
            return@map TestSystem.Compiler
        }

        if (file.contains("gradle")) {
            return@map TestSystem.Gradle
        }

        TestSystem.Unknown
    }.toMutableSet()

    if (TestSystem.Compiler in affected) {
        affected.add(TestSystem.Unknown)
    }

    return affected
}
