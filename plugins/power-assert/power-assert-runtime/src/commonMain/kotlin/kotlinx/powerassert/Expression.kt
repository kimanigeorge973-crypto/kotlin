/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

@ExperimentalPowerAssert
public abstract class Expression internal constructor(
    public val startOffset: Int,
    public val endOffset: Int,
    public val displayOffset: Int,
    public val value: Any?,
) {
    public abstract fun copy(deltaOffset: Int): Expression
}
