/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.Family
import templates.Family.*
import templates.PrimitiveType

fun collectionClassName(family: Family, primitive: PrimitiveType?): String = when (family) {
    Iterables, Sequences -> family.toString()
    ArraysOfObjects -> "Array"
    ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!}Array"
    else -> error(family)
}

fun constructorName(family: Family, primitive: PrimitiveType?): String = when (family) {
    Iterables -> "listOf"
    Sequences -> "sequenceOf"
    ArraysOfObjects -> "arrayOf"
    ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!.name.lowercase()}ArrayOf"
    else -> error(family)
}

fun forEachIsSortedFamily(action: (Family, PrimitiveType?) -> Unit) {
    action(Iterables, null)
    action(Sequences, null)
    action(ArraysOfObjects, null)
    for (primitive in PrimitiveType.defaultPrimitives) {
        action(ArraysOfPrimitives, primitive)
    }
    for (primitive in PrimitiveType.unsignedPrimitives) {
        action(ArraysOfUnsigned, primitive)
    }
}

fun swapAdjacentPair(values: List<String>): List<String> {
    val i = values.zipWithNext().indexOfFirst { (a, b) -> a != b }
    return values.toMutableList().apply { this[i] = this[i + 1].also { this[i + 1] = this[i] } }
}

class IsSortedTypeConfig(
    val sortedValues: List<String>,
    val unsortedValues: List<String> = swapAdjacentPair(sortedValues),
    val selectorExpr: String = "it",
    val selectorSortedValues: List<String> = sortedValues,
    val caseInsensitiveValues: Pair<List<String>, List<String>>? = null,
    val sampleSortedValues: List<String> = sortedValues,
    val sampleSelectorValues: List<String>,
    val sampleSelectorAssertions: List<Pair<String, Boolean>>,
)

private fun signedIntConfig(suffix: String, absExpr: String): IsSortedTypeConfig = IsSortedTypeConfig(
    sortedValues = (1..5).map { "$it$suffix" },
    sampleSelectorValues = listOf("1", "-2", "3", "-4", "5").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to true, absExpr to true, "it" to false)
)

private fun unsignedIntConfig(suffix: String, modExpr: String): IsSortedTypeConfig = IsSortedTypeConfig(
    sortedValues = (1..5).map { "$it$suffix" },
    sampleSelectorValues = listOf("3", "1", "4", "2").map { "$it$suffix" },
    sampleSelectorAssertions = listOf(modExpr to true, "it" to false)
)

private fun floatingPointConfig(suffix: String): IsSortedTypeConfig = IsSortedTypeConfig(
    sortedValues = listOf("1.0", "2.5", "3.14").map { "$it$suffix" },
    sampleSelectorValues = listOf("-0.5", "1.0", "-1.5", "2.0").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to true, "abs(it)" to true, "it" to false)
)

fun isSortedConfigFor(primitive: PrimitiveType?): IsSortedTypeConfig = when (primitive) {
    PrimitiveType.Char -> IsSortedTypeConfig(
        sortedValues = listOf("'a'", "'b'", "'c'"),
        sampleSelectorValues = listOf("'A'", "'b'", "'C'"),
        sampleSelectorAssertions = listOf("it.uppercaseChar()" to true, "it.lowercaseChar()" to true, "it" to false)
    )
    PrimitiveType.Boolean -> IsSortedTypeConfig(
        sortedValues = listOf("false", "true", "true"),
        selectorExpr = "it.compareTo(false)",
        sampleSortedValues = listOf("false", "false", "true"),
        sampleSelectorValues = listOf("false", "false", "true"),
        sampleSelectorAssertions = listOf("it.compareTo(false)" to true, "it" to true, "!it" to false)
    )
    null -> IsSortedTypeConfig(
        sortedValues = listOf("\"a\"", "\"b\"", "\"c\""),
        selectorExpr = "it.length",
        selectorSortedValues = listOf("\"a\"", "\"bb\"", "\"ccc\""),
        caseInsensitiveValues = listOf("\"Apple\"", "\"banana\"", "\"Cherry\"") to listOf("\"banana\"", "\"Apple\"", "\"Cherry\""),
        sampleSortedValues = listOf("\"apple\"", "\"banana\"", "\"cherry\""),
        sampleSelectorValues = listOf("\"c\"", "\"bb\"", "\"aaa\""),
        sampleSelectorAssertions = listOf("it.length" to true, "it" to false)
    )
    PrimitiveType.Byte, PrimitiveType.Short -> signedIntConfig(suffix = "", absExpr = "abs(it.toInt())")
    PrimitiveType.Int -> signedIntConfig(suffix = "", absExpr = "abs(it)")
    PrimitiveType.Long -> signedIntConfig(suffix = "L", absExpr = "abs(it)")
    PrimitiveType.Float -> floatingPointConfig(suffix = "f")
    PrimitiveType.Double -> floatingPointConfig(suffix = "")
    PrimitiveType.UByte, PrimitiveType.UShort -> unsignedIntConfig(suffix = "u", modExpr = "it.toUInt() % 3u")
    PrimitiveType.UInt -> unsignedIntConfig(suffix = "u", modExpr = "it % 3u")
    PrimitiveType.ULong -> unsignedIntConfig(suffix = "uL", modExpr = "it % 3uL")
}
