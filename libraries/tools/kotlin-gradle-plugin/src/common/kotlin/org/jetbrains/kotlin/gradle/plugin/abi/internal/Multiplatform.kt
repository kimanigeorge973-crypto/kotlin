/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

/**
 * Finalizes the configuration of the report variant for the Kotlin Multiplatform Gradle Plugin.
 */
internal fun AbiValidationExtension.finalizeMultiplatformVariant(
    project: Project,
    abiClasspath: Configuration,
    targets: NamedDomainObjectCollection<KotlinTarget>,
    keepLocallyUnsupportedTargets: Provider<Boolean>
) {
    val taskSet = AbiValidationTaskSet(project)
    taskSet.setClasspath(abiClasspath)
    taskSet.keepLocallyUnsupportedTargets(keepLocallyUnsupportedTargets)
    taskSet.klibEnabled(project.provider { true })

    val mavenPublications = project.collectPublications(binariesSource.get())
    project.processJvmKindTargets(binariesSource.get(), mavenPublications, targets, taskSet)
    project.processNonJvmTargets(binariesSource.get(), mavenPublications, targets, taskSet)
}


private fun Project.processJvmKindTargets(
    binariesSource: BinariesSource,
    mavenInfo: MavenInfo?,
    targets: Iterable<KotlinTarget>,
    abiValidationTaskSet: AbiValidationTaskSet,
) {
    // if there is only one JVM target then we will follow the shortcut
    val singleJvmTarget = targets.singleOrNull { target -> target.platformType == KotlinPlatformType.jvm }
    if (singleJvmTarget != null && targets.none { target -> target.platformType == KotlinPlatformType.androidJvm }) {
        addJvmInputs(
            binariesSource,
            abiValidationTaskSet,
            mavenInfo,
            singleJvmTarget.targetName,
            true,
            singleJvmTarget.compilations,
            KotlinCompilation.MAIN_COMPILATION_NAME
        )
        return
    }

    targets
        .asSequence()
        .filter { target -> target.platformType == KotlinPlatformType.jvm }
        .forEach { target ->
            addJvmInputs(
                binariesSource,
                abiValidationTaskSet,
                mavenInfo,
                target.targetName,
                false,
                target.compilations,
                KotlinCompilation.MAIN_COMPILATION_NAME
            )
        }

    targets
        .asSequence()
        .filterIsInstance<KotlinAndroidTarget>()
        .forEach { target ->
            addJvmInputs(
                binariesSource,
                abiValidationTaskSet,
                mavenInfo,
                target.targetName,
                false,
                target.compilations,
                ANDROID_RELEASE_BUILD_TYPE
            )
        }
}


private fun Project.processNonJvmTargets(
    binariesSource: BinariesSource,
    mavenInfo: MavenInfo?,
    targets: Iterable<KotlinTarget>,
    abiValidationTaskSet: AbiValidationTaskSet
) {
    val bannedInTests = bannedCanonicalTargetsInTest()
    targets
        .asSequence()
        .filter { target -> target.emitsKlib }
        .forEach { target ->
            val klibTarget = target.toKlibTarget()
            launch {
                if (target.targetIsSupported() && klibTarget.configurableName !in bannedInTests) {
                    addKlibInputs(
                        binariesSource,
                        abiValidationTaskSet,
                        mavenInfo,
                        klibTarget,
                        target.compilations
                    )
                } else {
                    abiValidationTaskSet.unsupportedTarget(klibTarget)
                }
            }
        }
}


private fun Project.addKlibInputs(
    binariesSource: BinariesSource,
    abiValidationTaskSet: AbiValidationTaskSet,
    mavenInfo: MavenInfo?,
    klibTarget: KlibTarget,
    compilations: NamedDomainObjectContainer<out KotlinCompilation<out Any>>,
) {
    when (binariesSource) {
        BinariesSource.MAVEN_PUBLICATIONS -> {
            val publication = mavenInfo!!.publications[klibTarget.targetName]
                ?: throw IllegalStateException("Publication for target ${klibTarget.configurableName} not found.\nSpecify `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")

            abiValidationTaskSet.addKlibTarget(klibTarget, files(publication.artifact))
            abiValidationTaskSet.addDependencies(publication.dependencies)
        }
        BinariesSource.MAIN_COMPILATION -> {
            compilations.withCompilationIfExists(KotlinCompilation.MAIN_COMPILATION_NAME) {
                abiValidationTaskSet.addKlibTarget(klibTarget, output.classesDirs)
            }
        }
        BinariesSource.NON_TEST_COMPILATIONS -> {
            compilations.configureEach { compilation ->
                if (!compilation.compilationName.contains("test", ignoreCase = true)) {
                    abiValidationTaskSet.addKlibTarget(klibTarget, compilation.output.classesDirs)
                }
            }
        }
    }
}

private fun Project.addJvmInputs(
    binariesSource: BinariesSource,
    abiValidationTaskSet: AbiValidationTaskSet,
    mavenInfo: MavenInfo?,
    targetName: String,
    singleTarget: Boolean,
    compilations: NamedDomainObjectContainer<out KotlinCompilation<out Any>>,
    mainCompilationName: String,
) {
    val classfiles = files()
    if (singleTarget) {
        abiValidationTaskSet.addSingleJvmTarget(classfiles)
    } else {
        abiValidationTaskSet.addJvmTarget(targetName, classfiles)
    }
    when (binariesSource) {
        BinariesSource.MAVEN_PUBLICATIONS -> {
            println("JVM publication: $targetName")
            val publication = mavenInfo!!.publications[targetName]
                ?: throw IllegalStateException("Publication for target $targetName not found.\nSpecify `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")

            println("Publication artifact: ${publication.artifact}")
            classfiles.from(publication.artifact)
            abiValidationTaskSet.addDependencies(publication.dependencies)
        }
        BinariesSource.MAIN_COMPILATION -> {
            compilations.withCompilationIfExists(mainCompilationName) {
                classfiles.from(output.classesDirs)
            }
        }
        BinariesSource.NON_TEST_COMPILATIONS -> {
            compilations.configureEach { compilation ->
                if (!compilation.compilationName.contains("test", ignoreCase = true)) {
                    classfiles.from(compilation.output.classesDirs)
                }
            }
        }
    }
}

private fun Project.collectPublications(binariesSource: BinariesSource): MavenInfo? {
    val publishingExtension = project.extensions.findByType(PublishingExtension::class.java)
    if (publishingExtension == null) {
        if (binariesSource == BinariesSource.MAVEN_PUBLICATIONS) {
            throw IllegalStateException("Source of binaries is set to Maven publications, but `maven-publish` plugin is not applied.\nPlease, apply `maven-publish` plugin or specify `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")
        }
        return null
    }

    val publications = publishingExtension.publications.filterIsInstance<MavenPublication>()

    val artifactPublications = publications.map { mavenPublication ->
        val mainArtifact = mavenPublication.artifacts.firstOrNull { it.classifier == null }
            ?: throw IllegalStateException("No main artifacts found in publication ${mavenPublication.name}.\nSpecify `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")

        PublicationInfo(mavenPublication.name, mainArtifact.file, mainArtifact.buildDependencies)
    }

    return MavenInfo(artifactPublications.associateBy { it.name })
}

internal class PublicationInfo(val name: String, val artifact: File, val dependencies: TaskDependency)

internal class MavenInfo(val publications: Map<String, PublicationInfo>)

private suspend fun KotlinTarget.targetIsSupported(): Boolean = when (this) {
    is KotlinNativeTarget -> crossCompilationOnCurrentHostSupported.await()
    else -> true
}

private fun Project.bannedCanonicalTargetsInTest(): Set<String> {
    val prop = kotlinPropertiesProvider.abiValidationBannedTargets
    prop ?: return emptySet()

    return prop.split(",").map { it.trim() }.toSet()
}
