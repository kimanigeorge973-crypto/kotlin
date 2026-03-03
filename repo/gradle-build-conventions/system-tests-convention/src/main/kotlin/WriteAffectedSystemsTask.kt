import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */



open class WriteAffectedSystemsTask : DefaultTask() {

    @get:Internal
    internal val diffService = project.featureBranchDiffService

    @get:Internal
    internal val affectedSystemsService = project.affectedTestSystemsService

    init {
        usesService(diffService)
        usesService(affectedSystemsService)
        outputs.upToDateWhen { false }
    }

    @OutputFile
    val diffFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.diffFile)

    @OutputFile
    val affectedSystemsFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.affectedSystemsFile)

    @TaskAction
    fun write() {
        val diffFile = diffFile.get().asFile.toPath()
        if (diffFile.exists()) {
            throw IllegalStateException("${diffFile.name} already exists")
        }
        diffFile.parent.createDirectories()
        diffFile.writeText(diffService.get().diff().joinToString(System.lineSeparator()))

        val affectedSystemsFile = affectedSystemsFile.get().asFile.toPath()
        if (affectedSystemsFile.exists()) {
            throw IllegalStateException("${affectedSystemsFile.name} already exists")
        }
        affectedSystemsFile.parent.createDirectories()
        affectedSystemsFile.writeText(affectedSystemsService.get().affectedTestSystems.joinToString(System.lineSeparator()))
    }
}
