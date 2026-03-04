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
    private val mappedName: String,
    private val isFinal: Boolean,
    private val substituteObjectWith: PsiType?,
    private val providedSignature: MethodSignature?,
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
    override fun getName(): String = mappedName

    override fun getParameterList(): PsiParameterList = cachedValue {
        LightParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            javaMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val typeFromJava = paramFromJava.type
                val providedType = providedSignature?.parameterTypes?.get(index)
                val candidateType = providedType ?: substituteType(typeFromJava)
                val shouldTryToUnbox = providedType != null ||
                        (typeFromJava.isJavaLangObject() && substituteObjectWith == candidateType) ||
                        typeFromJava.isTypeParameter()
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

    private fun PsiType.isJavaLangObject(): Boolean =
        this is PsiClassType && this.canonicalText == CommonClassNames.JAVA_LANG_OBJECT

    private fun PsiType.unboxedOrSelf(): PsiType =
        PsiPrimitiveType.getUnboxedType(this)?.annotate(TypeAnnotationProvider.EMPTY) ?: this

    private fun substituteType(psiType: PsiType): PsiType {
        val substituted = substitutor.substitute(psiType) ?: psiType
        return if (substituted.isJavaLangObject() && substituteObjectWith != null) {
            substituteObjectWith
        } else {
            substituted
        }
    }

    override fun getReturnType(): PsiType =
        providedSignature?.returnType ?: javaMethod.returnType?.let { substituteType(it) } ?: PsiTypes.voidType()

    override fun getTypeParameters(): Array<PsiTypeParameter> = javaMethod.typeParameters

    override fun getTypeParameterList(): PsiTypeParameterList? = javaMethod.typeParameterList

    override fun hasTypeParameters(): Boolean = javaMethod.hasTypeParameters()

    override fun hasModifierProperty(name: String): Boolean = when (name) {
        PsiModifier.ABSTRACT -> false // Always has implementation for overrides
        PsiModifier.FINAL -> isFinal
        PsiModifier.DEFAULT -> false
        else -> javaMethod.hasModifierProperty(name)
    }

    override fun isVarArgs(): Boolean = javaMethod.isVarArgs
}
