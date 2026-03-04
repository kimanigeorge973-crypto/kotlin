/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import javax.swing.Icon

/**
 * A light method for Kotlin collection method overrides or delegates that are mapped to Java collection methods
 * with special signatures.
 *
 * #### Example
 *
 * ```
 * abstract class MyCollection<Elem> : Collection<Elem> {
 *     override fun containsAll(elements: Collection<Elem>): Boolean {
 *         ...
 *     }
 * }
 *
 * abstract class MyCollection2<Elem> : Collection<Elem> by emptyList()
 * ```
 *
 * For the override and delegate methods `containsAll`, this class generates a light method with the remapped signature
 * `Collection<Elem>` -> `Collection<?>` that is expected by Java code.
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
        object : LightParameterListBuilder(manager, KotlinLanguage.INSTANCE) {
            override fun getParent(): PsiElement = this@SymbolLightMethodForMappedKotlinCollectionMethod
            override fun getElementIcon(flags: Int): Icon? = null
        }.apply {
            for (paramFromJava in javaMethod.parameterList.parameters) {
                val typeFromJava = paramFromJava.type
                val substitutedType = substituteType(typeFromJava)
                val type = if (typeFromJava.isTypeParameter()) substitutedType.unboxedOrSelf() else substitutedType

                addParameter(
                    object : LightParameter(
                        paramFromJava.name,
                        type,
                        this@SymbolLightMethodForMappedKotlinCollectionMethod,
                        KotlinLanguage.INSTANCE,
                        paramFromJava.isVarArgs
                    ) {
                        override fun getParent(): PsiElement = this@apply
                        override fun getElementIcon(flags: Int): Icon? = null
                    }
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
