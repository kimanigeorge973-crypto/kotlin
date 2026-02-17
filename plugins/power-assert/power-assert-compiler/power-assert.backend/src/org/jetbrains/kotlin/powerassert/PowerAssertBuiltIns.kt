/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.backend.utils.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

class PowerAssertBuiltIns(
    context: IrPluginContext,
) {
    companion object {
        const val PLUGIN_ID = "org.jetbrains.kotlin.powerassert"

        private fun dependencyError(message: String? = null): Nothing {
            if (message != null) {
                error("Power-Assert plugin runtime dependency was not found: $message")
            } else {
                error("Power-Assert plugin runtime dependency was not found.")
            }
        }

        private fun classId(identifier: String): ClassId =
            ClassId(packageFqName, Name.identifier(identifier))

        private fun classId(parent: ClassId, identifier: String): ClassId =
            parent.createNestedClassId(Name.identifier(identifier))

        private fun callableId(identifier: String): CallableId =
            CallableId(packageFqName, Name.identifier(identifier))

        private fun IrClassSymbol.primaryConstructor(): IrConstructorSymbol =
            constructors.singleOrNull { it.owner.isPrimary } ?: dependencyError()

        val packageFqName = FqName("kotlinx.powerassert")

        val powerAssertFqName = packageFqName.child(Name.identifier("PowerAssert"))
        val powerAssertClassId = ClassId.topLevel(powerAssertFqName)

        private val callExplanationFqName = packageFqName.child(Name.identifier("CallExplanation"))
        private val callExplanationClassId = ClassId.topLevel(callExplanationFqName)
        private val argumentClassId = classId(callExplanationClassId, "Argument")
        private val kindClassId = classId(argumentClassId, "Kind")
    }

    private val finder = context.finderForBuiltins()

    val metadata = PowerAssertMetadata(context.languageVersionSettings.languageVersion)

    private fun findClass(classId: ClassId): IrClassSymbol =
        finder.findClass(classId) ?: dependencyError(classId.toString())

    private fun findFunction(callableId: CallableId): IrSimpleFunctionSymbol =
        finder.findFunctions(callableId).singleOrNull() ?: dependencyError()

    val powerAssertClass = findClass(powerAssertClassId)
    val powerAssertType = powerAssertClass.defaultTypeWithoutArguments

    val powerAssertIgnoreClass = findClass(classId(powerAssertClassId, "Ignore"))
    val powerAssertIgnoreType = powerAssertIgnoreClass.defaultTypeWithoutArguments

    val expressionClass = findClass(classId("Expression"))
    val expressionType = expressionClass.defaultTypeWithoutArguments

    val valueExpressionClass = findClass(classId("ValueExpression"))
    val valueExpressionType = valueExpressionClass.defaultTypeWithoutArguments
    val valueExpressionConstructor = valueExpressionClass.primaryConstructor()

    val equalityExpressionClass = findClass(classId("EqualityExpression"))
    val equalityExpressionType = equalityExpressionClass.defaultTypeWithoutArguments
    val equalityExpressionConstructor = equalityExpressionClass.primaryConstructor()

    val callExplanationClass = findClass(callExplanationClassId)
    val callExplanationType = callExplanationClass.defaultTypeWithoutArguments
    val callExplanationConstructor = callExplanationClass.primaryConstructor()
    val toDefaultMessageFunction = findFunction(callableId("toDefaultMessage"))

    val argumentClass = findClass(argumentClassId)
    val argumentType = argumentClass.defaultTypeWithoutArguments
    val argumentConstructor = argumentClass.primaryConstructor()

    val argumentKindClass = findClass(kindClassId)
    val argumentKindType = argumentKindClass.defaultTypeWithoutArguments

    // -----

    val listOfFunction = finder.findFunctions(CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("listOf")))
        .singleOrNull { it.owner.parameters.firstOrNull()?.isVararg == true } ?: dependencyError()

    val jvmSyntheticAnnotation = finder.findConstructors(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID)
        .single()
}