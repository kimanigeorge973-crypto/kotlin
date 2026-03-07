/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * A service for converting [KaModule] to [Module] and vice versa.
 */
@KaImplementationDetail
public interface KaModuleConverter {
    @KaImplementationDetail
    public fun asKaModule(module: Module): KaModule?

    @KaImplementationDetail
    public fun asIDEAModule(module: KaModule): Module?

    @KaImplementationDetail
    public companion object {
        @KaImplementationDetail
        public fun getInstance(project: Project): KaModuleConverter? = project.serviceOrNull()
    }
}