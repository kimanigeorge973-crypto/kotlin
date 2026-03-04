/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.handlers.UpdateTestDataHandler
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.impl.FirstPhaseTestConfigurationImpl
import org.jetbrains.kotlin.test.impl.SecondPhaseTestConfigurationImpl
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.PrivateForInline
import kotlin.io.path.Path

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
abstract class TestConfigurationBuilderBase<B : TestConfigurationBuilderBase<B, C>, C : TestConfiguration<*>> {
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder()
    lateinit var assertions: AssertionsService

    protected val sourcePreprocessors: MutableList<Constructor<SourceFilePreprocessor>> = mutableListOf()
    protected val additionalMetaInfoProcessors: MutableList<Constructor<AdditionalMetaInfoProcessor>> = mutableListOf()
    protected val environmentConfigurators: MutableList<Constructor<AbstractEnvironmentConfigurator>> = mutableListOf()
    protected val preAnalysisHandlers: MutableList<Constructor<PreAnalysisHandler>> = mutableListOf()

    protected val additionalSourceProviders: MutableList<Constructor<AdditionalSourceProvider>> = mutableListOf()
    protected val moduleStructureTransformers: MutableList<Constructor<ModuleStructureTransformer>> = mutableListOf()

    protected val metaTestConfigurators: MutableList<Constructor<MetaTestConfigurator>> = mutableListOf()
    protected val afterAnalysisCheckers: MutableList<Constructor<AfterAnalysisChecker>> = mutableListOf()

    protected var metaInfoHandlerEnabled: Boolean = false

    protected val directives: MutableList<DirectivesContainer> = mutableListOf()
    val defaultRegisteredDirectivesBuilder: RegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    protected val configurationsByPositiveTestDataCondition: MutableList<Pair<Regex, B.() -> Unit>> = mutableListOf()
    protected val configurationsByNegativeTestDataCondition: MutableList<Pair<Regex, B.() -> Unit>> = mutableListOf()
    protected val additionalServices: MutableList<ServiceRegistrationData> = mutableListOf()

    protected var compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)? =
        null
    protected var runtimeClasspathProviders: MutableList<Constructor<RuntimeClasspathProvider>> = mutableListOf()

    protected val globalDefaultsConfigurators: MutableList<DefaultsProviderBuilder.() -> Unit> = mutableListOf()
    protected val defaultDirectiveConfigurators: MutableList<RegisteredDirectivesBuilder.() -> Unit> = mutableListOf()

    // ------------------------------------------------------------------------------------------------------------

    inline fun <reified T : TestService> useAdditionalService(noinline serviceConstructor: (TestServices) -> T) {
        useAdditionalServices(service(serviceConstructor))
    }

    fun useAdditionalServices(vararg serviceRegistrationData: ServiceRegistrationData) {
        additionalServices += serviceRegistrationData
    }

    fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        globalDefaultsConfigurators += init
        defaultsProviderBuilder.apply(init)
    }

    fun useSourcePreprocessor(vararg preprocessors: Constructor<SourceFilePreprocessor>, needToPrepend: Boolean = false) {
        if (needToPrepend) {
            sourcePreprocessors.addAll(0, preprocessors.toList())
        } else {
            sourcePreprocessors.addAll(preprocessors)
        }
    }

    fun useDirectives(vararg directives: DirectivesContainer) {
        this.directives += directives
    }

    fun useConfigurators(vararg environmentConfigurators: Constructor<AbstractEnvironmentConfigurator>) {
        this.environmentConfigurators += environmentConfigurators
    }

    fun usePreAnalysisHandlers(vararg handlers: Constructor<PreAnalysisHandler>) {
        this.preAnalysisHandlers += handlers
    }

    fun useMetaInfoProcessors(vararg updaters: Constructor<AdditionalMetaInfoProcessor>) {
        additionalMetaInfoProcessors += updaters
    }

    fun useAdditionalSourceProviders(vararg providers: Constructor<AdditionalSourceProvider>) {
        additionalSourceProviders += providers
    }

    @TestInfrastructureInternals
    fun useModuleStructureTransformers(vararg transformers: ModuleStructureTransformer) {
        for (transformer in transformers) {
            moduleStructureTransformers += { _ -> transformer }
        }
    }

    @TestInfrastructureInternals
    fun useModuleStructureTransformers(vararg transformers: Constructor<ModuleStructureTransformer>) {
        moduleStructureTransformers += transformers
    }

    @TestInfrastructureInternals
    fun useCustomCompilerConfigurationProvider(provider: (TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider) {
        compilerConfigurationProvider = provider
    }

    fun useCustomRuntimeClasspathProviders(vararg provider: Constructor<RuntimeClasspathProvider>) {
        runtimeClasspathProviders += provider
    }

    fun useMetaTestConfigurators(vararg configurators: Constructor<MetaTestConfigurator>) {
        metaTestConfigurators += configurators
    }

    fun useAfterAnalysisCheckers(vararg checkers: Constructor<AfterAnalysisChecker>, insertAtFirst: Boolean = false) {
        when (insertAtFirst) {
            false -> afterAnalysisCheckers += checkers
            true -> afterAnalysisCheckers.addAll(0, checkers.asList())
        }
    }

    fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultDirectiveConfigurators += init
        defaultRegisteredDirectivesBuilder.apply(init)
    }

    fun forTestsMatching(pattern: String, configuration: B.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        forTestsMatching(regex, configuration)
    }

    fun forTestsNotMatching(pattern: String, configuration: B.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        forTestsNotMatching(regex, configuration)
    }

    infix fun String.or(other: String): String {
        return """$this|$other"""
    }

    private fun String.toMatchingRegexString(): String = when (this) {
        "*" -> ".*"
        else -> """^.*/(${replace("*", ".*")})$"""
    }

    fun forTestsMatching(pattern: Regex, configuration: B.() -> Unit) {
        configurationsByPositiveTestDataCondition += pattern to configuration
    }

    fun forTestsNotMatching(pattern: Regex, configuration: B.() -> Unit) {
        configurationsByNegativeTestDataCondition += pattern to configuration
    }

    abstract fun build(testDataPath: String): C

    protected fun applyConditionalConfigurations(testDataPath: String) {
        // We use URI here because we use '/' in our codebase, and URI also uses it (unlike OS-dependent `toString()`)
        val absoluteTestDataPath = Path(testDataPath).normalize().toUri().toString()

        for ((regex, configuration) in configurationsByPositiveTestDataCondition) {
            if (regex.matches(absoluteTestDataPath)) {
                @Suppress("UNCHECKED_CAST")
                configuration(this as B)
            }
        }
        for ((regex, configuration) in configurationsByNegativeTestDataCondition) {
            if (!regex.matches(absoluteTestDataPath)) {
                @Suppress("UNCHECKED_CAST")
                configuration(this as B)
            }
        }
    }

}

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
sealed class OnePhaseTestConfigurationBuilderBase<
        B : TestConfigurationBuilderBase<B, C>,
        C : TestConfiguration<*>,
        > : TestConfigurationBuilderBase<B, C>() {
    private typealias Step = TestStep<*, *>
    private typealias StepBuilder = TestStepBuilder<*, *, Step>

    @PrivateForInline
    val steps: MutableList<StepBuilder> = mutableListOf()

    @PrivateForInline
    val namedSteps: MutableMap<String, StepBuilder> = mutableMapOf()
}

@OptIn(PrivateForInline::class)
class FirstPhaseTestConfigurationBuilder :
    OnePhaseTestConfigurationBuilderBase<FirstPhaseTestConfigurationBuilder, FirstPhaseTestConfiguration>() {
    lateinit var testInfo: KotlinTestInfo
    lateinit var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.FirstPhase<I, O> {
        return TestStepBuilder.FacadeStepBuilder.FirstPhase(facade).also {
            steps.add(it)
        }
    }

    inline fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return TestStepBuilder.HandlersStepBuilder.FirstPhase(artifactKind, compilationStage).also {
            it.init()
            steps += it
        }
    }

    inline fun <InputArtifact, InputArtifactKind> namedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        val previouslyContainedStep = namedStepOfType<InputArtifact, InputArtifactKind>(name)
        return if (previouslyContainedStep == null) {
            val step = handlersStep(artifactKind, compilationStage, init)
            namedSteps[name] = step
            step
        } else {
            configureNamedHandlersStep(name, artifactKind, skipMissingStep = false, init)
            previouslyContainedStep
        }
    }

    inline fun <InputArtifact, InputArtifactKind> configureNamedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        skipMissingStep: Boolean = false,
        init: TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ) where InputArtifact : ResultingArtifact<InputArtifact>,
            InputArtifactKind : TestArtifactKind<InputArtifact> {
        val step = namedStepOfType<InputArtifact, InputArtifactKind>(name)
            ?: when (skipMissingStep) {
                true -> return
                false -> error("Step \"$name\" not found")
            }
        require(step.artifactKind == artifactKind) { "Step kind: ${step.artifactKind}, passed kind is $artifactKind" }
        step.apply(init)
    }

    fun <InputArtifact, InputArtifactKind> namedStepOfType(name: String): TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>?
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        return namedSteps[name] as TestStepBuilder.HandlersStepBuilder.FirstPhase<InputArtifact, InputArtifactKind>?
    }

    fun enableMetaInfoHandler() {
        metaInfoHandlerEnabled = true
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun build(testDataPath: String): FirstPhaseTestConfiguration {
        applyConditionalConfigurations(testDataPath)

        // UpdateTestDataHandler should be _the very last_ handler at all times to avoid false-positive test data changes,
        // so it is added after all configuration callbacks have already been executed
        useAfterAnalysisCheckers(::UpdateTestDataHandler)

        @Suppress("UNCHECKED_CAST")
        return FirstPhaseTestConfigurationImpl(
            testInfo,
            defaultsProviderBuilder.build(),
            assertions,
            steps as List<TestStepBuilder<*, *, TestStep.FirstPhaseStep<*, *>>>,
            sourcePreprocessors,
            additionalMetaInfoProcessors,
            environmentConfigurators,
            additionalSourceProviders,
            preAnalysisHandlers,
            moduleStructureTransformers,
            metaTestConfigurators,
            afterAnalysisCheckers,
            compilerConfigurationProvider,
            runtimeClasspathProviders,
            metaInfoHandlerEnabled,
            directives,
            defaultRegisteredDirectivesBuilder.build(),
            startingArtifactFactory,
            additionalServices,
            originalBuilder = ReadOnlyBuilder(this, testDataPath)
        )
    }

    class ReadOnlyBuilder(private val builder: FirstPhaseTestConfigurationBuilder, val testDataPath: String) {
        val assertions: AssertionsService
            get() = builder.assertions
        val sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>
            get() = builder.sourcePreprocessors
        val additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>
            get() = builder.additionalMetaInfoProcessors
        val environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>
            get() = builder.environmentConfigurators
        val directives: List<DirectivesContainer>
            get() = builder.directives

        val defaultDirectiveConfigurators: List<RegisteredDirectivesBuilder.() -> Unit>
            get() = builder.defaultDirectiveConfigurators

        val globalDefaultsConfigurators: List<DefaultsProviderBuilder.() -> Unit>
            get() = builder.globalDefaultsConfigurators

        val additionalServices: List<ServiceRegistrationData>
            get() = builder.additionalServices

        val compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?
            get() = builder.compilerConfigurationProvider
        val testInfo: KotlinTestInfo
            get() = builder.testInfo
        val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
            get() = builder.startingArtifactFactory
    }
}

typealias TestConfigurationBuilder = FirstPhaseTestConfigurationBuilder

@OptIn(PrivateForInline::class)
class SecondPhaseTestConfigurationBuilder :
    OnePhaseTestConfigurationBuilderBase<SecondPhaseTestConfigurationBuilder, SecondPhaseTestConfiguration>() {
    lateinit var testInfo: KotlinTestInfo
    val mergerWorkers: MutableList<Constructor<SecondPhaseInputsMerger.Worker>> = mutableListOf()

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractSecondPhaseTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.SecondPhase<I, O> {
        return TestStepBuilder.FacadeStepBuilder.SecondPhase(facade).also {
            steps.add(it)
        }
    }

    inline fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return TestStepBuilder.HandlersStepBuilder.SecondPhase(artifactKind, compilationStage).also {
            it.init()
            steps += it
        }
    }

    inline fun <InputArtifact, InputArtifactKind> namedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        val previouslyContainedStep = namedStepOfType<InputArtifact, InputArtifactKind>(name)
        return if (previouslyContainedStep == null) {
            val step = handlersStep(artifactKind, compilationStage, init)
            namedSteps[name] = step
            step
        } else {
            configureNamedHandlersStep(name, artifactKind, skipMissingStep = false, init)
            previouslyContainedStep
        }
    }

    inline fun <InputArtifact, InputArtifactKind> configureNamedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        skipMissingStep: Boolean = false,
        init: TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ) where InputArtifact : ResultingArtifact<InputArtifact>,
            InputArtifactKind : TestArtifactKind<InputArtifact> {
        val step = namedStepOfType<InputArtifact, InputArtifactKind>(name)
            ?: when (skipMissingStep) {
                true -> return
                false -> error("Step \"$name\" not found")
            }
        require(step.artifactKind == artifactKind) { "Step kind: ${step.artifactKind}, passed kind is $artifactKind" }
        step.apply(init)
    }

    fun <InputArtifact, InputArtifactKind> namedStepOfType(name: String): TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>?
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        return namedSteps[name] as TestStepBuilder.HandlersStepBuilder.SecondPhase<InputArtifact, InputArtifactKind>?
    }

    fun withMergerWorker(worker: Constructor<SecondPhaseInputsMerger.Worker>) {
        mergerWorkers += worker
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun build(testDataPath: String): SecondPhaseTestConfiguration {
        applyConditionalConfigurations(testDataPath)

        // UpdateTestDataHandler should be _the very last_ handler at all times to avoid false-positive test data changes,
        // so it is added after all configuration callbacks have already been executed
        useAfterAnalysisCheckers(::UpdateTestDataHandler)

        @Suppress("UNCHECKED_CAST")
        return SecondPhaseTestConfigurationImpl(
            testInfo,
            defaultsProviderBuilder.build(),
            assertions,
            steps as List<TestStepBuilder<*, *, TestStep.SecondPhaseStep<*, *>>>,
            sourcePreprocessors,
            additionalMetaInfoProcessors,
            environmentConfigurators,
            additionalSourceProviders,
            preAnalysisHandlers,
            moduleStructureTransformers,
            metaTestConfigurators,
            afterAnalysisCheckers,
            compilerConfigurationProvider,
            runtimeClasspathProviders,
            metaInfoHandlerEnabled,
            directives,
            defaultRegisteredDirectivesBuilder.build(),
            mergerWorkers,
            additionalServices,
        )
    }
}

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
class TwoPhaseTestConfigurationBuilder {
    val firstPhaseBuilder = FirstPhaseTestConfigurationBuilder()
    val secondPhaseBuilder = SecondPhaseTestConfigurationBuilder()

    fun commonConfiguration(init: TestConfigurationBuilderBase<*, *>.() -> Unit) {
        firstPhaseBuilder.apply(init)
        secondPhaseBuilder.apply(init)
    }

    fun firstPhase(init: FirstPhaseTestConfigurationBuilder.() -> Unit) {
        firstPhaseBuilder.apply(init)
    }

    fun secondPhase(init: SecondPhaseTestConfigurationBuilder.() -> Unit) {
        secondPhaseBuilder.apply(init)
    }
}

inline fun testConfiguration(testDataPath: String, init: FirstPhaseTestConfigurationBuilder.() -> Unit): FirstPhaseTestConfiguration {
    return FirstPhaseTestConfigurationBuilder().apply(init).build(testDataPath)
}
