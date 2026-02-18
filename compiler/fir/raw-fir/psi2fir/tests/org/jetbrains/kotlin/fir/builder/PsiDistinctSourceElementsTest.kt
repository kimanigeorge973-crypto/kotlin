/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.fir.checkDistinctSourceElements
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithoutTestData
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class PsiDistinctSourceElementsTest : AbstractRawFirBuilderTestCase() {
    /**
     * Walks all Kotlin source files in the repository (excluding test data) and checks that the source elements of FIR declarations are
     * distinct via [checkDistinctSourceElements].
     */
    fun testTotalKotlin() {
        val root = File(testDataPath)

        testDataPath.walkRepositoryKotlinFilesWithoutTestData { file ->
            val ktFile = createKtFile(file.toRelativeString(root))
            val firFile = ktFile.toFirFile()

            checkDistinctSourceElements(listOf(firFile)) { _, _ -> "Duplicate source elements in '${file.toRelativeString(root)}'" }
        }
    }
}
