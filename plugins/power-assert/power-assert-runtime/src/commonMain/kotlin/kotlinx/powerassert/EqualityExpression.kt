/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

@ExperimentalPowerAssert
public class EqualityExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
    public val lhs: Any?,
    public val rhs: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(deltaOffset: Int): EqualityExpression {
        return EqualityExpression(
            startOffset = startOffset + deltaOffset,
            endOffset = endOffset + deltaOffset,
            displayOffset = displayOffset + deltaOffset,
            value = value,
            lhs = lhs,
            rhs = rhs,
        )
    }

    override fun toString(): String {
        return "EqualityExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value, lhs=$lhs, rhs=$rhs)"
    }
}
