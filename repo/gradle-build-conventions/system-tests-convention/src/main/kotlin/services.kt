import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.systemTest.SystemTestMode
import org.jetbrains.kotlin.systemTest.TestSystem
import org.jetbrains.kotlin.systemTest.currentAffectedTestSystems
import org.jetbrains.kotlin.systemTest.currentSystemTestModeOrNull
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.io.path.Path

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


internal val Project.affectedSystemBuildService: Provider<AffectedSystemBuildService>
    get() = gradle.sharedServices.registerIfAbsent("affectedSystemBuildService", AffectedSystemBuildService::class.java)

abstract class AffectedSystemBuildService : BuildService<BuildServiceParameters.None> {
    @get:Inject
    abstract val exec: ExecOperations

    val affectedTestSystems: Set<TestSystem> by lazy {
        /* Precedence goes to currentAffectedTestSystems, which is provided by the current environment (e.g. by command line) */
        currentAffectedTestSystems?.let { return@lazy it }

        val out = ByteArrayOutputStream()
        exec.exec {
            commandLine("git", "diff", "--name-only", "origin/master...HEAD")
            standardOutput = out
        }.assertNormalExitValue().rethrowFailure()

        val changedFiles = out.toByteArray().decodeToString().lines().map { changeEntry -> Path(changeEntry) }
        affectedTestSystems(changedFiles).toSet()
    }
}


abstract class SystemTestModeValueSource : ValueSource<SystemTestMode, SystemTestModeValueSource.Params> {
    interface Params : ValueSourceParameters {
        val testSystem: Property<TestSystem>
        val service: Property<AffectedSystemBuildService>
    }

    override fun obtain(): SystemTestMode? {
        /* Precedence goes to currentSystemTestModeOrNull, which is provided by the current environment (e.g. by command line) */
        currentSystemTestModeOrNull?.let { return it }

        val testSystem = parameters.testSystem.get()
        if (testSystem == TestSystem.Unknown) return SystemTestMode.Full

        return if (parameters.testSystem.get() in parameters.service.get().affectedTestSystems) {
            SystemTestMode.Full
        } else {
            SystemTestMode.Smoke
        }
    }
}

abstract class AffectedTestSystemValueSource : ValueSource<Set<TestSystem>, AffectedTestSystemValueSource.Params> {
    interface Params : ValueSourceParameters {
        val service: Property<AffectedSystemBuildService>
    }

    override fun obtain(): Set<TestSystem> {
        return parameters.service.get().affectedTestSystems
    }
}
