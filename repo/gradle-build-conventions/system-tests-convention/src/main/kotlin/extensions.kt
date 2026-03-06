import org.gradle.api.Project
import org.gradle.api.provider.*
import org.gradle.api.tasks.testing.Test
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.systemTest.*
import org.jetbrains.kotlin.systemTest.gradle.affectedTestSystemsService
import org.jetbrains.kotlin.systemTest.gradle.testSystem

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Test.withSmokeTestPattern(@Language("RegExp") pattern: String) {
    systemProperty(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN, pattern)
    environment(SYSTEM_TEST_SMOKE_TEST_FQN_PATTERN_ENV_KEY, pattern)
}

val Project.systemTestMode: Provider<SystemTestMode>
    get() {
        val myTestSystem = project.testSystem
        return project.affectedTestSystems.map { affectedTestSystems ->
            if (myTestSystem in affectedTestSystems) {
                SystemTestMode.Full
            } else SystemTestMode.Smoke
        }

    }

val Project.affectedTestSystems: Provider<Set<TestSystem>>
    get() {
        return providers.environmentVariable(SYSTEM_TEST_AFFECTED_ENV_KEY)
            .map { it.split(";").map { TestSystem.valueOf(it) }.toSet() }
            .orElse(project.affectedTestSystemsService.map { it.affectedTestSystems })

        /*
        providers.of(AffectedTestSystemValueSource::class.java) {
            parameters.service.set(project.affectedTestSystemsService.map { it.affectedTestSystems })
        }*/
    }


val Project.isSystemTestFederationEnabled: Provider<Boolean>
    get() = provider { currentSystemTestFederationEnabledOrNull }
        .orElse(project.providers.gradleProperty(SYSTEM_TEST_FEDERATION_ENABLED_KEY).map { it.toBoolean() })


abstract class AffectedTestSystemValueSource : ValueSource<Set<TestSystem>, AffectedTestSystemValueSource.Params> {
    interface Params : ValueSourceParameters {
        val service: SetProperty<TestSystem>
    }

    override fun obtain(): Set<TestSystem>? {
        val raw = System.getenv(SYSTEM_TEST_AFFECTED_ENV_KEY)
        return raw?.split(";")?.map { TestSystem.valueOf(it) }?.toSet() ?: parameters.service.get()
    }
}
