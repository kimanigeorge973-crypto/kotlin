/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.finder

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.module.Module
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement

/**
 * Provides a JVM-module which should be used as a context for light-class creation in [JavaElementFinder].
 */
interface KaLightClassesModuleHelper {
    fun getSuitableJvmModule(element: KtElement, searchScope: GlobalSearchScope): Response

    sealed class Response {
        /**
         * A default (use-site) module should be used
         */
        object SHOULD_USE_DEFAULT : Response()

        /**
         * A valid module is not found, should not create light class for the given [KtClassOrObject].
         */
        object MODULE_NOT_FOUND : Response()

        /**
         * An adjusted JVM [module] was found.
         */
        class MODULE_FOUND(val module: Module) : Response()
    }

    companion object {
        fun getSuitableJvmModule(element: KtElement, searchScope: GlobalSearchScope): Response =
            element.project.serviceOrNull<KaLightClassesModuleHelper>()?.getSuitableJvmModule(element, searchScope)
                ?: Response.SHOULD_USE_DEFAULT
    }
}