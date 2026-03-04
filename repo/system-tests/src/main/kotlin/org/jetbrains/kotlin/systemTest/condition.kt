/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.systemTest

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils
import kotlin.jvm.optionals.getOrNull

class SystemTestExecutionCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        when (currentSystemTestMode) {
            SystemTestMode.Full -> return enabled("System Test Mode: '$currentSystemTestMode'")
            SystemTestMode.Smoke -> Unit
        }

        val isSmokeTest = context.findAnnotation<SmokeTest>()?.isSmokeTest ?: run pattern@{
            val pattern = currentSmokeTestPattern ?: return@pattern false
            val testClass = context.testClass.getOrNull() ?: return@pattern false
            testClass.name.replace(".", "/").matches(pattern)
        }

        if (isSmokeTest) return enabled("Smoke Test")

        /* Not a smoke test: Find if it is a contract test */
        val currentAffectedTestSystems = currentAffectedTestSystems ?: return enabled("No changed systems")

        val contracts = context.findRepeatableAnnotation<ContractTest>().map { it.testSystem }.toSet()
        if (contracts.isEmpty()) return disabled("Not a smoke test")

        val changedContractSystem = contracts.intersect(currentAffectedTestSystems)
        if (changedContractSystem.isEmpty()) return disabled("No contracts affected")

        return enabled(
            "Affected Contracts: ${changedContractSystem.joinToString(separator = ", ") { it.name }}"
        )
    }
}


private inline fun <reified T : Annotation> ExtensionContext.findAnnotation(): T? {
    AnnotationUtils.findAnnotation(element, T::class.java).getOrNull()?.let { return it }
    val testClass = testClass.getOrNull() ?: return null
    return AnnotationUtils.findAnnotation(testClass, T::class.java, true).getOrNull()
}

private inline fun <reified T : Annotation> ExtensionContext.findRepeatableAnnotation(): List<T> {
    return buildList {
        addAll(AnnotationUtils.findRepeatableAnnotations<T>(testMethod, T::class.java))
        var clazz: Class<*>? = testClass.getOrNull()
        while (clazz != null) {
            addAll(AnnotationUtils.findRepeatableAnnotations<T>(clazz, T::class.java))
            clazz = clazz.superclass
        }
    }
}
