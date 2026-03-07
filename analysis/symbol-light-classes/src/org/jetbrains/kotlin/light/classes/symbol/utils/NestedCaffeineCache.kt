/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.jetbrains.kotlin.analysis.api.platform.caches.getOrPut

class NestedCaffeineCache<A : Any, B : Any, C : Any>(
    private val innerFactory: () -> Cache<B, C> = {
        Caffeine.newBuilder()
            .weakKeys()
            .weakValues()
            .build()
    }
) {
    private val outerCache: Cache<A, Cache<B, C>> =
        Caffeine.newBuilder()
            .weakKeys()
            .build<A, Cache<B, C>>()

    fun getOrPut(firstKey: A, secondKey: B, compute: (A, B) -> C?): C? {
        val innerCache = outerCache.getOrPut(firstKey) { innerFactory() }
        return innerCache.get(secondKey) { secondKeyValue ->
            compute(firstKey, secondKeyValue)
        }
    }

    fun invalidateAll() {
        outerCache.invalidateAll()
    }
}
