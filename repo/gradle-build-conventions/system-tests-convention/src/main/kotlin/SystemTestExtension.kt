package org.jetbrains.kotlin.systemTest.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

open class SystemTestExtension @Inject constructor(private val project: Project) {
    val defaultDependencyEnabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
}
