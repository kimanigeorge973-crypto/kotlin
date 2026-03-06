/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.systemTest

import org.junit.experimental.categories.Category
import org.junit.jupiter.api.Tag

@Tag("smoke")
annotation class SmokeTest(val isSmokeTest: Boolean = true)

@Repeatable
annotation class ContractTest(val testSystem: TestSystem)

@Repeatable
@Tag("contract:Compiler")
annotation class CompilerContractTest
