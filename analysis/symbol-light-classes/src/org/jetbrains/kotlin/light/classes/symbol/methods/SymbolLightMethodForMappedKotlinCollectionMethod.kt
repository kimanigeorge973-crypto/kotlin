/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.isTypeParameter

/**
 * A light method for Kotlin collection method overrides that are mapped to Java collection methods
 * with special signatures.
 *
 * This class extends [SymbolLightSimpleMethod] to:
 * - Inherit annotation handling (preserves user-defined annotations from Kotlin source)
 * - Override parameter list and return type with Java collection method mapping logic
 *
 * Used when a Kotlin class overrides a collection method (e.g., `containsAll`) that needs to be
 * represented with a Java-compatible signature while preserving the original Kotlin annotations.
 */
internal class SymbolLightMethodForMappedKotlinCollectionMethod(
    functionSymbol: KaNamedFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassForClassOrObject,
    private val javaMethod: PsiMethod,
    private val substitutor: PsiSubstitutor,
    private val isFinal: Boolean,
) : SymbolLightSimpleMethod(
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = METHOD_INDEX_BASE,
    isTopLevel = false,
    valueParameterPickMask = null,
    suppressStatic = false,
    isJvmExposedBoxed = false,
) {
    override fun getParameterList(): PsiParameterList = cachedValue {
        LightParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            javaMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val typeFromJava = paramFromJava.type
                val candidateType = substituteType(typeFromJava)
                val shouldTryToUnbox = typeFromJava.isTypeParameter()
                val type = if (shouldTryToUnbox) candidateType.unboxedOrSelf() else candidateType

                addParameter(
                    LightParameter(
                        paramFromJava.name,
                        type,
                        this@SymbolLightMethodForMappedKotlinCollectionMethod,
                        KotlinLanguage.INSTANCE,
                        paramFromJava.isVarArgs
                    )
                )
            }
        }
    }

    private fun PsiType.unboxedOrSelf(): PsiType =
        PsiPrimitiveType.getUnboxedType(this)?.annotate(TypeAnnotationProvider.EMPTY) ?: this

    private fun substituteType(psiType: PsiType): PsiType =
        substitutor.substitute(psiType) ?: psiType

    override fun getReturnType(): PsiType =
        javaMethod.returnType?.let { substituteType(it) } ?: PsiTypes.voidType()

    override fun hasModifierProperty(name: String): Boolean = when (name) {
        PsiModifier.FINAL -> isFinal
        else -> super.hasModifierProperty(name)
    }
}
