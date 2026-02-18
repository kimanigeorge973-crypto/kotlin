/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import kotlin.collections.forEach

/**
 * Checks that all FIR declarations in the given [files] have distinct [KtSourceElement]s. This is a requirement of FIR as Data (KT-84343)
 * and checked in various tests.
 *
 * @param lazyErrorHeadline A headline for the error message if the check fails. The parameters are the two FIR declarations that have
 *  conflicting source elements.
 */
inline fun checkDistinctSourceElements(files: List<FirFile>, crossinline lazyErrorHeadline: (FirDeclaration, FirDeclaration) -> String) {
    val declarationBySourceElement = mutableMapOf<KtSourceElement, FirDeclaration>()

    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                checkDeclaration(element)
            }

            element.acceptChildren(this)
        }

        private fun checkDeclaration(declaration: FirDeclaration) {
            val sourceElement = declaration.symbol.source ?: return
            val previousDeclaration = declarationBySourceElement.put(sourceElement, declaration)

            // We have to compare `previousDeclaration` and `declaration` with reference equality, because regular equality defers to the
            // source element whose uniqueness we want to check in the first place.
            //
            // Source element uniqueness doesn't prevent the *exact* same FIR declaration instance from appearing multiple times. It's about
            // different FIR declaration instances sharing equal source elements accidentally.
            if (previousDeclaration != null && previousDeclaration !== declaration) {
                error(
                    "${lazyErrorHeadline(previousDeclaration, declaration)}:\n" +
                            "  First declaration: ${previousDeclaration::class.simpleName} at ${previousDeclaration.source}\n" +
                            "  Second declaration: ${declaration::class.simpleName} at ${declaration.source}"
                )
            }
        }
    }

    files.forEach { it.accept(visitor) }
}
