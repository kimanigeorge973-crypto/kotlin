/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

import kotlin.jvm.JvmStatic

/**
 * ```
 * @PowerAssert
 * fun assert(condition: Boolean) {
 *     if (!condition) {
 *         val explanation = PowerAssert.explanation
 *         throw AssertionError(explanation?.toDefaultMessage() ?: "Assertion failed")
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class PowerAssert {
    public companion object {
        @Suppress("RedundantNullableReturnType")
        @JvmStatic
        public val explanation: CallExplanation?
            get() = throw NotImplementedError("Intrinsic property! Make sure the Power-Assert compiler-plugin is applied to your build.")
    }

    /**
     * ```
     * @PowerAssert
     * fun assert(
     *     condition: Boolean,
     *     // Parameter `message` will not have an explanation generated at call-site.
     *     @PowerAssert.Ignore message: String? = null,
     * )
     * ```
     *
     * ```
     * @PowerAssert.Ignore // Parameters of type AssertionBuilder are automatically ignored.
     * class AssertionBuilder<T>(val subject: T)
     *
     * @PowerAssert
     * fun AssertionBuilder<*>.isEqualTo(value: Any?)
     * ```
     */
    // TODO plugin supports configurable annotations (Spring-Boot might be good use case)
    // TODO support meta-annotations?
    // TODO should temporary variables of types annotated with @Ignore be considered pseudo-constants?
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    @MustBeDocumented
    public annotation class Ignore
}
