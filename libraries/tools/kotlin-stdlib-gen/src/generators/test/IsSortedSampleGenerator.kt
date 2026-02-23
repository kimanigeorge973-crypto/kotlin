/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.*
import templates.Family.*
import templates.PrimitiveType
import java.io.BufferedWriter
import java.io.File

object IsSortedSampleGenerator {
    fun generate() {
        generate(Iterables)
        generate(Sequences)
        generate(ArraysOfObjects)
        for (primitive in PrimitiveType.defaultPrimitives) {
            generate(ArraysOfPrimitives, primitive)
        }
        for (primitive in PrimitiveType.unsignedPrimitives) {
            generate(ArraysOfUnsigned, primitive)
        }
    }

    private class TypeConfig(
        val sortedValues: List<String>,
        val selectorValues: List<String>,
        val selectorAssertions: List<Pair<String, Boolean>>,
        val withAssertions: List<Pair<String, Boolean>> = selectorAssertions,
        val descSelectorValues: List<String> = selectorValues.reversed(),
        val unsortedValues: List<String> = swapAdjacentPair(sortedValues),
    )

    private fun signedIntConfig(suffix: String, absExpr: String): TypeConfig = TypeConfig(
        sortedValues = (1..5).map { "$it$suffix" },
        selectorValues = listOf("1", "-2", "3", "-4", "5").map { "$it$suffix" },
        selectorAssertions = listOf("it * it" to true, absExpr to true, "it" to false)
    )

    private fun unsignedIntConfig(suffix: String, modExpr: String): TypeConfig = TypeConfig(
        sortedValues = (1..5).map { "$it$suffix" },
        selectorValues = listOf("3", "1", "4", "2").map { "$it$suffix" },
        selectorAssertions = listOf(modExpr to true, "it" to false)
    )

    private fun floatingPointConfig(suffix: String): TypeConfig = TypeConfig(
        sortedValues = listOf("1.0", "2.5", "3.14").map { "$it$suffix" },
        selectorValues = listOf("-0.5", "1.0", "-1.5", "2.0").map { "$it$suffix" },
        selectorAssertions = listOf("it * it" to true, "abs(it)" to true, "it" to false)
    )

    private fun configFor(elementType: String): TypeConfig = when (elementType) {
        "Byte", "Short" -> signedIntConfig(suffix = "", absExpr = "abs(it.toInt())")
        "Int" -> signedIntConfig(suffix = "", absExpr = "abs(it)")
        "Long" -> signedIntConfig(suffix = "L", absExpr = "abs(it)")
        "Float" -> floatingPointConfig("f")
        "Double" -> floatingPointConfig("")
        "UByte", "UShort" -> unsignedIntConfig(suffix = "u", modExpr = "it.toUInt() % 3u")
        "UInt" -> unsignedIntConfig(suffix = "u", modExpr = "it % 3u")
        "ULong" -> unsignedIntConfig(suffix = "uL", modExpr = "it % 3uL")
        "Char" -> TypeConfig(
            sortedValues = listOf("'a'", "'b'", "'c'"),
            selectorValues = listOf("'A'", "'b'", "'C'"),
            selectorAssertions = listOf("it.uppercaseChar()" to true, "it.lowercaseChar()" to true, "it" to false)
        )
        "Boolean" -> TypeConfig(
            sortedValues = listOf("false", "false", "true"),
            selectorValues = listOf("false", "false", "true"),
            selectorAssertions = listOf("it.compareTo(false)" to true, "it" to true, "!it" to false),
            withAssertions = listOf("it.toString()" to true, "it" to true, "!it" to false),
            descSelectorValues = listOf("true", "true", "false"),
            unsortedValues = listOf("true", "false", "true")
        )
        "T" -> TypeConfig(
            sortedValues = listOf("\"apple\"", "\"banana\"", "\"cherry\""),
            selectorValues = listOf("\"c\"", "\"bb\"", "\"aaa\""),
            selectorAssertions = listOf("it.length" to true, "it" to false)
        )
        else -> error(elementType)
    }

    private fun swapAdjacentPair(values: List<String>): List<String> {
        val mid = values.size / 2
        return values.toMutableList().apply { this[mid - 1] = this[mid].also { this[mid] = this[mid - 1] } }
    }

    private fun generate(family: Family, primitive: PrimitiveType? = null) {
        val collectionClass = when (family) {
            Iterables, Sequences -> family.toString()
            ArraysOfObjects -> "Array"
            ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!}Array"
            else -> error(family)
        }

        val isGeneric = family in listOf(Iterables, Sequences, ArraysOfObjects)
        val elementType = if (isGeneric) "T" else primitive!!.toString()

        val ctor = when (family) {
            Iterables -> "listOf"
            Sequences -> "sequenceOf"
            ArraysOfObjects -> "arrayOf"
            ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!.name.lowercase()}ArrayOf"
        }

        val config = configFor(elementType)
        val sorted = config.sortedValues
        val unsorted = config.unsortedValues
        val className = "IsSorted${collectionClass}Samples"
        val file = File("libraries/stdlib/samples/test/samples/generated/issorted/$className.kt")
        file.parentFile.mkdirs()
        file.bufferedWriter().use { writer ->
            writer.apply {
                val needsAbsImport = config.selectorAssertions.any { it.first.contains("abs(") }
                writeHeader(className, needsAbsImport)

                writeSimpleSample("isSorted", ctor, sorted, unsorted)

                val descSorted = sorted.reversed()
                val descUnsorted = swapAdjacentPair(descSorted)
                writeSimpleSample("isSortedDescending", ctor, descSorted, descUnsorted)

                writeSelectorSample("isSortedWith", ctor, config.selectorValues, config.withAssertions) { selector ->
                    "isSortedWith(compareBy { $selector })"
                }

                writeSelectorSample("isSortedBy", ctor, config.selectorValues, config.selectorAssertions) { selector ->
                    "isSortedBy { $selector }"
                }

                writeSelectorSample(
                    "isSortedByDescending", ctor, config.descSelectorValues, config.selectorAssertions
                ) { selector ->
                    "isSortedByDescending { $selector }"
                }

                appendLine("}")
            }
        }
    }

    private fun BufferedWriter.writeHeader(className: String, needsAbsImport: Boolean) {
        appendLine(COPYRIGHT_NOTICE)
        appendLine()
        appendLine("package samples.generated.issorted")
        appendLine()
        appendLine(autoGeneratedWarning("IsSortedSampleGenerator.kt"))
        appendLine()
        appendLine("import samples.*")
        if (needsAbsImport) {
            appendLine("import kotlin.math.abs")
        }
        appendLine()
        appendLine("class $className {")
    }

    private fun BufferedWriter.writeSimpleSample(
        name: String,
        collectionOf: String,
        sortedValues: List<String>,
        unsortedValues: List<String>,
    ) {
        val sortedArgs = sortedValues.joinToString(", ")
        val unsortedArgs = unsortedValues.joinToString(", ")
        appendLine(
            """
    @Sample
    fun $name() {
        val sorted = $collectionOf($sortedArgs)
        assertPrints(sorted.$name(), "true")

        val unsorted = $collectionOf($unsortedArgs)
        assertPrints(unsorted.$name(), "false")
    }"""
        )
    }

    private fun BufferedWriter.writeSelectorSample(
        name: String,
        collectionOf: String,
        selectorValues: List<String>,
        assertions: List<Pair<String, Boolean>>,
        callExpr: (String) -> String,
    ) {
        val args = selectorValues.joinToString(", ")
        val assertLines = assertions.joinToString("\n") { (selector, expected) ->
            "        assertPrints(values.${callExpr(selector)}, \"$expected\")"
        }
        appendLine(
            """
    @Sample
    fun $name() {
        val values = $collectionOf($args)
$assertLines
    }"""
        )
    }
}
