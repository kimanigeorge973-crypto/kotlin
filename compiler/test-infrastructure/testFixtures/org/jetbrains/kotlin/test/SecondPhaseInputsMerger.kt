/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo

class SecondPhaseInputsMerger(val testServices: TestServices, val workers: List<Worker>) {
    fun merge(firstPhaseOutputs: List<FirstPhaseOutput>): SecondPhaseInputArtifact {
        val secondPhaseConfiguration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)
        workers.forEach { worker ->
            worker.process(secondPhaseConfiguration, firstPhaseOutputs.map { it.testServices })
        }
        return SecondPhaseInputArtifact(secondPhaseConfiguration, firstPhaseOutputs)
    }

    abstract class Worker(val testServices: TestServices) {
        abstract fun process(configuration: CompilerConfiguration, firstPhaseServices: List<TestServices>)
    }
}

data class FirstPhaseOutput(
    val testServices: TestServices,
    val catchingExecutor: CatchingExecutor,
) {
    val testInfo: KotlinTestInfo get() = testServices.testInfo

    fun interface CatchingExecutor {
        fun executeWithCatching(block: () -> Unit)
    }
}


class SecondPhaseInputArtifact(
    val secondPhaseConfiguration: CompilerConfiguration,
    val firstPhaseOutputs: List<FirstPhaseOutput>
) : ResultingArtifact<SecondPhaseInputArtifact>() {
    object Kind : TestArtifactKind<SecondPhaseInputArtifact>("SecondPhaseInputArtifact")

    override val kind: Kind get() = Kind
}

class SecondStageInputsHolder(val firstPhaseOutputs: List<FirstPhaseOutput>) : TestService

private val TestServices.secondStageInputsHolder: SecondStageInputsHolder by TestServices.testServiceAccessor()
val TestServices.secondPhaseInputs: List<FirstPhaseOutput>
    get() = secondStageInputsHolder.firstPhaseOutputs
