import org.gradle.api.Project
import org.jetbrains.kotlin.systemTest.TestSystem
import java.nio.file.Path

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


internal val Project.testSystem: TestSystem
    get() {
        return when {
            this.path.contains("gradle") -> TestSystem.Gradle
            this.path.contains("compiler") -> TestSystem.Compiler
            else -> TestSystem.Unknown
        }
    }


internal fun affectedTestSystems(changedFiles: List<Path>): Set<TestSystem> {
    val affected = mutableSetOf<TestSystem>()

    if (changedFiles.any { it.toString().contains("compiler") }) {
        affected.add(TestSystem.Compiler)
    }

    if (changedFiles.any { it.toString().contains("gradle") }) {
        affected.add(TestSystem.Gradle)
    }

    if (TestSystem.Compiler in affected) {
        affected.add(TestSystem.Unknown)
    }

    return affected
}
