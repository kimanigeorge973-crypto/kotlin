/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.fileClasses.isJvmMultifileClassFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

abstract class KotlinAsJavaSupportSharedBase<TModule : Any>(protected val project: Project) : KotlinAsJavaSupport() {
    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightFacade(file: KtFile, module: TModule): KtLightClassForFacade? {
        if (!file.facadeIsPossible()) return null

        val facadeFqName = file.javaFileFacadeFqName
        val facadeFiles = if (file.canHaveAdditionalFilesInFacade()) {
            findFilesForFacade(facadeFqName, module.contentSearchScope).filter(KtFile::isJvmMultifileClassFile)
        } else {
            listOf(file)
        }

        return when {
            facadeFiles.none(KtFile::hasTopLevelCallables) -> null
            facadeFiles.none(KtFile::isCompiled) -> {
                createInstanceOfLightFacade(facadeFqName, module, facadeFiles)
            }

            facadeFiles.all(KtFile::isCompiled) -> {
                createInstanceOfDecompiledLightFacade(facadeFqName, facadeFiles)
            }

            else -> error("Source and compiled files are mixed: $facadeFiles")
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass? {
        if (classOrObject.shouldNotBeVisibleAsLightClass()) return null

        val containingFile = classOrObject.containingKtFile
        when (declarationLocation(containingFile)) {
            DeclarationLocation.ProjectSources -> {
                return createInstanceOfLightClass(classOrObject, module)
            }

            DeclarationLocation.LibraryClasses -> {
                return createInstanceOfDecompiledLightClass(classOrObject, module)
            }

            DeclarationLocation.LibrarySources -> {
                val originalClassOrObject = ApplicationManager.getApplication()
                    .getService(KotlinDeclarationNavigationPolicy::class.java)
                    ?.getOriginalElement(classOrObject) as? KtClassOrObject

                val value = originalClassOrObject?.takeUnless(classOrObject::equals)?.let {
                    guardedRun { getLightClass(it, module) }
                }

                return value
            }

            null -> Unit
        }

        if (containingFile.analysisContext != null || containingFile.originalFile.virtualFile != null) {
            return createInstanceOfLightClass(classOrObject, module)
        }

        return null
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightScript(script: KtScript, module: TModule?): KtLightClass? {
        val containingFile = script.containingFile
        if (containingFile is KtCodeFragment) {
            // Avoid building light classes for code fragments
            return null
        }

        return createInstanceOfLightScript(script, module)
    }

    /**
     * lightweight applicability check
     */
    protected fun KtFile.facadeIsPossible(): Boolean = when {
        isCompiled && !name.endsWith(".class") -> false
        isScript() -> false
        canHaveAdditionalFilesInFacade() -> true
        else -> hasTopLevelCallables()
    }

    private fun KtFile.canHaveAdditionalFilesInFacade(): Boolean = !isCompiled && isJvmMultifileClassFile

    protected abstract fun KtFile.findModule(): TModule?
    protected abstract fun facadeIsApplicable(module: TModule, file: KtFile): Boolean
    protected abstract val TModule.contentSearchScope: GlobalSearchScope

    protected abstract fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade?

    protected open fun createInstanceOfLightFacade(facadeFqName: FqName, module: TModule, files: List<KtFile>): KtLightClassForFacade? {
        return createInstanceOfLightFacade(facadeFqName, files)
    }

    protected abstract fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade?

    abstract fun projectWideOutOfBlockModificationTracker(): ModificationTracker

    open fun outOfBlockModificationTracker(element: PsiElement): ModificationTracker {
        return projectWideOutOfBlockModificationTracker()
    }


    protected abstract fun createInstanceOfLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass?
    protected abstract fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass?
    protected abstract fun declarationLocation(file: KtFile): DeclarationLocation?
    protected abstract fun createInstanceOfLightScript(script: KtScript, module: TModule?): KtLightClass?
    protected abstract fun getLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass?

    protected enum class DeclarationLocation {
        ProjectSources, LibraryClasses, LibrarySources,
    }

    override fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade {
        return createInstanceOfLightFacade(file.javaFileFacadeFqName, listOf(file)) ?: errorWithAttachment(
            "Unsupported ${file::class.simpleName}"
        ) {
            withEntry("module", file.findModule().toString())
            withPsiEntry("file", file)
        }
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacadeByPackage(packageFqName, scope).toFacadeClasses()
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacade(facadeFqName, scope).toFacadeClasses()
    }

    private fun Collection<KtFile>.toFacadeClasses(): List<KtLightClassForFacade> = mapNotNull { file ->
        file.takeIf { it.facadeIsPossible() }?.findModule()?.let { file to it }
    }.groupBy { (file, module) ->
        FacadeKey(file.javaFileFacadeFqName, file.isJvmMultifileClassFile, module)
    }.mapNotNull { (_, pairs) ->
        pairs.firstNotNullOfOrNull { (file, module) ->
            file.takeIf { facadeIsApplicable(module, file) }
        }?.let(::getLightFacade)
    }

    protected data class FacadeKey<TModule>(val fqName: FqName, val isMultifile: Boolean, val module: TModule)

    private val recursiveGuard = ThreadLocal<Boolean>()
    private inline fun <T> guardedRun(body: () -> T): T? {
        if (recursiveGuard.get() == true) return null
        return try {
            recursiveGuard.set(true)
            body()
        } finally {
            recursiveGuard.set(false)
        }
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        return findFilesForFacadeByPackage(packageFqName, scope).mapNotNullTo(mutableSetOf()) { file ->
            file.takeIf { it.facadeIsPossible() }
                ?.takeIf { it.findModule()?.let { module -> facadeIsApplicable(module, file) } == true }
                ?.javaFileFacadeFqName
                ?.shortName()
                ?.asString()
        }.toSet()
    }

    override fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        if (scriptFqName.isRoot) {
            return emptyList()
        }

        return findFilesForScript(scriptFqName, scope).mapNotNull { getLightClassForScript(it, null) }
    }

    abstract fun librariesTracker(element: PsiElement): ModificationTracker

    protected inline fun <T : PsiElement, V> ifValid(element: T, action: () -> V?): V? {
        ProgressManager.checkCanceled()

        return if (!element.isValid)
            null
        else
            action()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinAsJavaSupportSharedBase<*> {
            return KotlinAsJavaSupport.getInstance(project) as KotlinAsJavaSupportSharedBase<*>
        }
    }
}

