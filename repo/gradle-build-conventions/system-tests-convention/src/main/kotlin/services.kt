package org.jetbrains.kotlin.systemTest.gradle

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.systemTest.TestSystem
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


internal val Project.featureBranchDiffService: Provider<FeatureBranchDiffService>
    get() = gradle.sharedServices.registerIfAbsent("featureBranchDiffService", FeatureBranchDiffService::class.java) {
        parameters.diffFile.set(project.diffFile)
    }

internal val Project.affectedTestSystemsService: Provider<AffectedTestSystemsService>
    get() = gradle.sharedServices.registerIfAbsent("affectedSystemBuildService", AffectedTestSystemsService::class.java) {
        parameters.diffService.set(featureBranchDiffService)
        parameters.affectedSystemsFile.set(affectedSystemsFile)
    }

internal val Project.diffFile
    get() = isolated.rootProject.projectDirectory.file(".test-system.diff.txt")

internal val Project.affectedSystemsFile
    get() = isolated.rootProject.projectDirectory.file(".test-system.affected.txt")

abstract class FeatureBranchDiffService : BuildService<FeatureBranchDiffService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val diffFile: RegularFileProperty
    }

    @get:Inject
    internal abstract val exec: ExecOperations

    private var _diff: List<String>? = null

    fun diff(): List<String> {
        _diff?.let { return it }
        _diff = calculateFeatureBranchChangedFiles()
        return _diff.orEmpty()
    }

    private fun calculateFeatureBranchChangedFiles(): List<String> {
        /*
        A diff file might be provided by CI environments
         */
        if (parameters.diffFile.get().asFile.exists()) {
            return parameters.diffFile.get().asFile.readLines()
        }

        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val result = exec.exec {
            commandLine("git", "diff", "--name-only", "origin/master...HEAD")
            isIgnoreExitValue = true
            standardOutput = out
            errorOutput = err
        }

        if (result.exitValue != 0) throw Exception(
            "Inferring changed fails (git diff) failed with exit code ${result.exitValue}\n" + err.toByteArray().decodeToString()
        )

        return out.toByteArray().decodeToString().lines()
    }

    @Synchronized
    override fun close() {
        _diff = null
    }
}


abstract class AffectedTestSystemsService : BuildService<AffectedTestSystemsService.Params>, AutoCloseable {

    interface Params : BuildServiceParameters {
        val diffService: Property<FeatureBranchDiffService>
        val affectedSystemsFile: RegularFileProperty
    }

    private var cachedValue: Set<TestSystem>? = null

    @get:Synchronized
    val affectedTestSystems: Set<TestSystem>
        get() {
            cachedValue?.let { return it }

            /*
            A .affected-systems.txt file might be provided by CI environments
            */
            if (parameters.affectedSystemsFile.get().asFile.exists()) {
                return parameters.affectedSystemsFile.get().asFile.readLines().map { TestSystem.valueOf(it) }.toSet()
            }

            val value = affectedTestSystems(parameters.diffService.get().diff())
            cachedValue = value
            return value
        }


    @Synchronized
    override fun close() {
        cachedValue = null
    }
}
