import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.fus.internal.isCiBuild
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import kotlin.io.path.Path
import kotlin.io.path.readLines

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

enum class System {
    KotlinGradlePlugin,
    Compiler,
    Other
}

internal val Project.system: System
    get() {
        return when {
            this.path.contains("gradle") -> System.KotlinGradlePlugin
            this.path.contains("compiler") -> System.Compiler
            else -> System.Other
        }
    }


@get:Synchronized
internal val Project.teamcityBuildChangedSystems: Set<System>?
    get() {
        val key = "teamcityBuildAffectedSystems"
        if (gradle.extraProperties.has(key)) {
            @Suppress("UNCHECKED_CAST")
            return gradle.extraProperties[key] as Set<System>?
        }

        val affectedSystems = run {
            if (!isCiBuild()) return@run null
            val changedFilesPath = java.lang.System.getenv("TEAMCITY_CHANGED_FILES_PATH")
            if (changedFilesPath == null) {
                logger.warn("'TEAMCITY_CHANGED_FILES_PATH' is not set")
                return@run null
            }

            val changedFiles = Path(changedFilesPath).readLines()
            val changedSystems = System.entries.associateWith { false }.toMutableMap()

            changedFiles.forEach { changeEntry ->
                if (changeEntry.startsWith("libraries/tools/kotlin-gradle")) {
                    changedSystems[System.KotlinGradlePlugin] = true
                } else if (changeEntry.startsWith("compiler/")) {
                    changedSystems[System.Compiler] = true
                } else {
                    changedSystems[System.Other] = true
                }
            }

            changedSystems.filterValues { it }.keys.toSet()
        }

        logger.quiet("Changed Systems: $affectedSystems")
        gradle.extraProperties[key] = affectedSystems
        return affectedSystems
    }
