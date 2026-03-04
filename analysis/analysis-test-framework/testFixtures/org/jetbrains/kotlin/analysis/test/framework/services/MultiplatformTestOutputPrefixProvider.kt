/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.TestInfrastructureInternals

/**
 * A test prefix provider for multiplatform tests.
 *
 * The resulting list of prefixes is produced based on
 * the original list of prefixes and the target platform set in the test configuration.
 * The resulting list is sorted from the least specific prefix to the most specific.
 *
 * For original prefix `standalone.fir` and JS target platform
 * [MultiplatformTestOutputPrefixProvider] will produce `standalone.fir`, `standalone.fir.knm`, `knm`, `standalone.fir.js`, `js`.
 * `knm` in this case represents all non-JVM platforms.
 *
 * The COMMON platform doesn't have its own `common` prefix and is mapped to just `knm`.
 * So for the same original prefix `standalone.fir` and COMMON target platform
 * [MultiplatformTestOutputPrefixProvider] will produce `standalone.fir`, `standalone.fir.knm`, `knm`.
 *
 * Note that when the JVM target platform is set, the provider returns the original list of prefixes.
 * That's because the JVM platform is considered a default platform and should not require additional prefixes.
 */
object MultiplatformTestOutputPrefixProvider {
    fun getPrefixes(originalPrefixes: List<String>, targetPlatform: TargetPlatform): List<String> {
        @OptIn(TestInfrastructureInternals::class)
        if (targetPlatform.isJvm()) {
            return originalPrefixes
        }

        val knmPrefix = "knm"

        return buildList {
            addAll(originalPrefixes)
            addAll(originalPrefixes.map { "$it.$knmPrefix" })
            add(knmPrefix)

            if (!targetPlatform.isCommon()) {
                val platformPrefix = when {
                    targetPlatform.isJs() -> "js"
                    targetPlatform.isNative() -> "native"
                    targetPlatform.isWasm() -> "wasm"
                    else -> error("Unsupported platform $targetPlatform")
                }

                addAll(originalPrefixes.map { "$it.$platformPrefix" })
                add(platformPrefix)
            }
        }
    }
}