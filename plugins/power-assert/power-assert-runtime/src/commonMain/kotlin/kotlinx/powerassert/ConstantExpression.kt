/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

@ExperimentalPowerAssert
public class ConstantExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(deltaOffset: Int): ConstantExpression {
        return ConstantExpression(
            startOffset = startOffset + deltaOffset,
            endOffset = endOffset + deltaOffset,
            displayOffset = displayOffset + deltaOffset,
            value = value,
        )
    }

    override fun toString(): String {
        return "ConstantExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value)"
    }
}
