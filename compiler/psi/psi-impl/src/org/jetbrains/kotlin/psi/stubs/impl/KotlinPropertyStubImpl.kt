/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinPropertyStubImpl(
    parent: StubElement<*>?,
    private val name: StringRef?,
    override val isVar: Boolean,
    override val isTopLevel: Boolean,
    override val hasDelegate: Boolean,
    override val hasDelegateExpression: Boolean,
    override val hasInitializer: Boolean,
    override val isExtension: Boolean,
    override val hasReturnTypeRef: Boolean,
    override val fqName: FqName?,
    val constantInitializer: ConstantValue<*>?,
    val origin: KotlinStubOrigin?,
    override val hasBackingField: Boolean?,
) : KotlinStubBaseImpl<KtProperty>(parent, KtStubElementTypes.PROPERTY), KotlinPropertyStub {

    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level properties")
        }
        if (hasDelegateExpression && !hasDelegate) {
            throw IllegalArgumentException("Can't have delegate expression without delegate")
        }
    }

    override fun getName(): String? = StringRef.toString(name)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinPropertyStubImpl = KotlinPropertyStubImpl(
        parent = newParent,
        name = name,
        isVar = isVar,
        isTopLevel = isTopLevel,
        hasDelegate = hasDelegate,
        hasDelegateExpression = hasDelegateExpression,
        hasInitializer = hasInitializer,
        isExtension = isExtension,
        hasReturnTypeRef = hasReturnTypeRef,
        fqName = fqName,
        constantInitializer = constantInitializer,
        origin = origin,
        hasBackingField = hasBackingField,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean {
        if (other !is KotlinPropertyStubImpl) return false
        if (this.name != other.name) return false
        if (this.fqName != other.fqName) return false
        if (this.isTopLevel != other.isTopLevel) return false
        if (this.hasDelegate != other.hasDelegate) return false
        if (this.hasDelegateExpression != other.hasDelegateExpression) return false
        if (this.hasInitializer != other.hasInitializer) return false
        if (this.isExtension != other.isExtension) return false
        if (this.hasReturnTypeRef != other.hasReturnTypeRef) return false
        if (this.hasBackingField != other.hasBackingField) return false
        if (this.origin != other.origin) return false
        if (this.isVar != other.isVar) return false
        return this.constantInitializer == other.constantInitializer
    }
}
