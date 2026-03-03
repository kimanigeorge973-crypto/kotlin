/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Finalizes the configuration of the report variant for the JVM version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationExtension.finalizeJvmVariant(
    project: Project,
    abiClasspath: Configuration,
    target: KotlinTarget,
) {
    val publicationInfo = project.findPublication(binariesSource.get())
    finalizeVariant(project, binariesSource.get(), publicationInfo, abiClasspath, MAIN_COMPILATION_NAME, target)
}


/**
 * Finalizes the configuration of the report variant for the Android version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationExtension.finalizeAndroidVariant(
    project: Project,
    abiClasspath: Configuration,
    target: KotlinTarget,
) {
    val publicationInfo = project.findPublication(binariesSource.get())
    finalizeVariant(project, binariesSource.get(), publicationInfo, abiClasspath, ANDROID_RELEASE_BUILD_TYPE, target)
}

private fun finalizeVariant(
    project: Project,
    binariesSource: BinariesSource,
    publicationInfo: PublicationInfo?,
    abiClasspath: Configuration,
    compilationName: String,
    target: KotlinTarget
) {
    val taskSet = AbiValidationTaskSet(project)
    taskSet.setClasspath(abiClasspath)

    val classfiles = project.files()
    taskSet.addSingleJvmTarget(classfiles)

    when (binariesSource) {
        BinariesSource.MAVEN_PUBLICATIONS -> {
            classfiles.from(publicationInfo!!.artifact)
            taskSet.addDependencies(publicationInfo.dependencies)
        }
        BinariesSource.MAIN_COMPILATION -> {
            target.compilations.withCompilationIfExists(compilationName) {
                classfiles.from(output.classesDirs)
            }
        }
        BinariesSource.NON_TEST_COMPILATIONS -> {
            target.compilations.configureEach { compilation ->
                if (!compilation.compilationName.contains("test", ignoreCase = true)) {
                    classfiles.from(compilation.output.classesDirs)
                }
            }
        }
    }
}

private fun Project.findPublication(binariesSource: BinariesSource): PublicationInfo? {
    if (binariesSource != BinariesSource.MAVEN_PUBLICATIONS) return null

    val publishingExtension = extensions.findByType(PublishingExtension::class.java)
        ?: throw IllegalStateException("Source of binaries is set to Maven publications, but there is no any publication.\nPlease, apply `maven-publish` plugin and create Maven publication, or specify `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")

    val publications = publishingExtension.publications.filterIsInstance<MavenPublication>()
    val mainPublications = publications.filter { pub -> pub.artifacts.any { artifact -> artifact.classifier == null } }

    if (mainPublications.size > 1) {
        throw IllegalStateException("There are more than one Maven publication with the default classifier. Please, specify which one to use via `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")
    }

    if (mainPublications.isEmpty()) {
        throw IllegalStateException("Source of binaries is set to Maven publications, but there is no any publication.\nPlease, apply `maven-publish` plugin and create Maven publication, or specify `kotlin.abiValidation { binariesSource = MAIN_COMPILATION }` to use output of the main compilation tasks")
    }

    val main = mainPublications.single()

    val mainArtifact = main.artifacts.first { it.classifier == null }
    return PublicationInfo(main.name, mainArtifact.file, mainArtifact.buildDependencies)
}
