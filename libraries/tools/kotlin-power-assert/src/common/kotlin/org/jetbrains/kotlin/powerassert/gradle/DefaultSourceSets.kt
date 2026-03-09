/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.gradle

enum class DefaultSourceSets {
    /**
     * All Kotlin SourceSets should be transformed by default.
     */
    ALL,

    /**
     * Only test Kotlin SourceSets should be transformed by default.
     */
    TEST,

    /**
     * No Kotlin SourceSets should be transformed by default.
     */
    NONE,
}
