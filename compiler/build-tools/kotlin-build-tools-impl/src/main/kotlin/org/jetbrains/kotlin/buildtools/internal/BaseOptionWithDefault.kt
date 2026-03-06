/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption

internal abstract class BaseOptionWithDefault<V> private constructor(
    id: String,
    private val hasDefault: Boolean = false,
    private val default: V? = null,
    optionsRegistry: MutableMap<String, BaseOptionWithDefault<*>>,
) : BaseOption<V>(id) {
    constructor(id: String, optionsRegistry: MutableMap<String, BaseOptionWithDefault<*>>) : this(id, false, null, optionsRegistry)
    constructor(id: String, default: V, optionsRegistry: MutableMap<String, BaseOptionWithDefault<*>>) : this(
        id,
        true,
        default,
        optionsRegistry
    )

    init {
        assert(optionsRegistry[id] == null)
        optionsRegistry[id] = this
    }

    @Suppress("UNCHECKED_CAST")
    val defaultValue: V
        get() = if (hasDefault) {
            default as V
        } else {
            error("Value is not set for $id and it has no default value")
        }
}