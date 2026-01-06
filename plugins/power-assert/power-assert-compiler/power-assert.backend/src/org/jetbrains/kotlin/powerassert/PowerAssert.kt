/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

val EXPLAIN_BLOCK by IrStatementOriginImpl
val EXPLAIN_TEMPORARY by IrDeclarationOriginImpl.Synthetic

fun IrValueDeclaration.isExplained(): Boolean {
    val variable = this as? IrVariable ?: return false
    val initializer = variable.initializer as? IrContainerExpression ?: return false
    return initializer.origin == EXPLAIN_BLOCK
}
