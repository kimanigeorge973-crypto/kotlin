/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

import kotlin.jvm.JvmStatic

public class CallExplanation(
    override val offset: Int,
    override val source: String,
    public val arguments: List<Argument>,
) : Explanation() {
    override val expressions: List<Expression>
        get() = arguments.sortedBy { it.startOffset }.flatMap { it.expressions }

    override fun toString(): String {
        return "CallExplanation(offset=$offset, source='$source', arguments=$arguments)"
    }

    public class Argument
    @PublishedApi internal constructor(
        public val startOffset: Int,
        public val endOffset: Int,
        public val kind: Kind,
        public val expressions: List<Expression>,
    ) {
        override fun toString(): String {
            return "Argument(startOffset=$startOffset, endOffset=$endOffset, kind=$kind, expressions=$expressions)"
        }

        public enum class Kind {
            DISPATCH,
            CONTEXT,
            EXTENSION,
            VALUE,
        }
    }

    @PowerAssert.Ignore
    public companion object {
        @JvmStatic
        @PowerAssert
        @Suppress("UNUSED_PARAMETER")
        public fun <T> of(value: T): Pair<T, CallExplanation> {
            error("Power-Assert compiler-plugin must be applied to project to use this function.")
        }

        @JvmStatic
        @Deprecated(level = DeprecationLevel.HIDDEN, message = "Manual implementation for binary compatibility.")
        @Suppress("FunctionName")
        public fun <T> `of$powerassert`(value: T, explanation: CallExplanation): Pair<T, CallExplanation> {
            return value to explanation
        }
    }
}
