/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleConverter
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiled.light.classes.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportSharedBase
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.finder.KaLightClassesModuleHelper
import org.jetbrains.kotlin.fileClasses.isJvmMultifileClassFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolBasedFakeLightClass
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForScript
import org.jetbrains.kotlin.light.classes.symbol.classes.createSymbolLightClassNoCache
import org.jetbrains.kotlin.light.classes.symbol.utils.NestedCaffeineCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import java.util.*

private val KMP_CACHE: ThreadLocal<MutableMap<KtElement, KtLightClass?>> = ThreadLocal.withInitial { null }

private val isMultiplatformSupportAvailable: Boolean
    get() = KMP_CACHE.get() != null

/**
 * Enables light classes in non-JVM modules inside the given [block].
 *
 * The provided light classes might not correctly represent non-JVM concepts.
 * E.g., while class types provide qualified class names, [com.intellij.psi.impl.source.PsiClassReferenceType.resolve] might return
 * `false`, as in non-JVM modules there is usually no configured JDK.
 *
 * The method is designed to be used only for UAST (see https://plugins.jetbrains.com/docs/intellij/uast.html) in Android Lint.
 */
@KaNonPublicApi
@RequiresReadLock
fun <T> withMultiplatformLightClassSupport(project: Project, block: () -> T): T {
    if (isMultiplatformSupportAvailable) {
        // Allow reentrant access
        return block()
    }

    val permissionChecker = KaAnalysisPermissionChecker.getInstance(project)
    check(permissionChecker.isAnalysisAllowed()) {
        val rejectionReason = permissionChecker.getRejectionReason()
        "Cannot enable multiplatform light class support. $rejectionReason"
    }

    try {
        KMP_CACHE.set(WeakHashMap())
        return block()
    } finally {
        KMP_CACHE.set(null)
    }
}

@Deprecated(
    "Use withMultiplatformLightClassSupport(project, block) instead",
    ReplaceWith("withMultiplatformLightClassSupport(project, block)")
)
@KaNonPublicApi
@RequiresReadLock
fun <T> withMultiplatformLightClassSupport(block: () -> T): T {
    if (isMultiplatformSupportAvailable) {
        // Allow reentrant access
        return block()
    }

    require(ApplicationManager.getApplication().isReadAccessAllowed) { "The method can only run inside a read action" }
    require(!ApplicationManager.getApplication().isWriteAccessAllowed) { "The method cannot be run inside a write action" }

    try {
        KMP_CACHE.set(WeakHashMap())
        return block()
    } finally {
        KMP_CACHE.set(null)
    }
}

private fun KaModule.isLightClassSupportAvailable(): Boolean {
    return targetPlatform.has<JvmPlatform>() || isMultiplatformSupportAvailable
}

internal class SymbolKotlinAsJavaSupport(project: Project) : KotlinAsJavaSupportSharedBase<KaModule>(project) {

    init {
        // It's generally best practice to register listeners in plugin XMLs. However, we don't know whether the platform intends to
        // implement a caching package provider factory or not, and thus can't register these listeners in the XML.
        project.analysisMessageBus.connect(project).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                when (event) {
                    is KotlinModuleStateModificationEvent,
                    is KotlinModuleOutOfBlockModificationEvent,
                    KotlinGlobalModuleStateModificationEvent,
                    KotlinGlobalSourceModuleStateModificationEvent,
                    KotlinGlobalScriptModuleStateModificationEvent,
                    KotlinGlobalSourceOutOfBlockModificationEvent,
                        -> lightClassCache.invalidateAll()

                    is KotlinCodeFragmentContextModificationEvent -> {}
                }
            }
        )
    }

    @KaCachedService
    private val projectStructureProvider by lazyPub { KotlinProjectStructureProvider.getInstance(project) }

    private fun PsiElement.getModuleIfSupportEnabled(): KaModule? {
        return projectStructureProvider.getModule(
            element = this,
            useSiteModule = null,
        ).takeIf(KaModule::isLightClassSupportAvailable)
    }

    @OptIn(KaImplementationDetail::class)
    private fun PsiElement.convertModuleOrDefaultIfSupportEnabled(module: Module?): KaModule? {
        val kaModule = module?.let { ideaModule ->
            KaModuleConverter.getInstance(ideaModule.project)?.asKaModule(ideaModule)
        } ?: projectStructureProvider.getModule(
            element = this,
            useSiteModule = null,
        )

        return kaModule.takeIf(KaModule::isLightClassSupportAvailable)
    }

    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> = project.createDeclarationProvider(searchScope, contextualModule = null).run {
        getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName).flatMap {
            getAllClassesByClassId(ClassId.topLevel(packageFqName.child(it)))
        }
    }

    override fun findFilesForPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> = buildSet {
        addAll(project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacadeByPackage(packageFqName))
        findClassOrObjectDeclarationsInPackage(packageFqName, searchScope).mapTo(this) {
            it.containingKtFile
        }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacadeByPackage(packageFqName)
    }

    override fun findFilesForScript(scriptFqName: FqName, searchScope: GlobalSearchScope): Collection<KtScript> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForScript(scriptFqName)
    }

    private fun FqName.toClassIdSequence(): Sequence<ClassId> {
        var currentName = shortNameOrSpecial()
        if (currentName.isSpecial) return emptySequence()
        var currentParent = parentOrNull() ?: return emptySequence()
        var currentRelativeName = currentName.asString()

        return sequence {
            while (true) {
                yield(ClassId(currentParent, FqName(currentRelativeName), isLocal = false))
                currentName = currentParent.shortNameOrSpecial()
                if (currentName.isSpecial) break
                currentParent = currentParent.parentOrNull() ?: break
                currentRelativeName = "${currentName.asString()}.$currentRelativeName"
            }
        }
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        val declarationProvider = project.createDeclarationProvider(searchScope, contextualModule = null)
        return fqName.toClassIdSequence()
            .flatMap(declarationProvider::getAllClassesByClassId)
            .filter { it.isFromSourceOrLibraryBinary() }
            .toSet()
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean =
        project.createPackageProvider(scope).doesKotlinOnlyPackageExist(fqName)

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> =
        project.createPackageProvider(scope)
            .getKotlinOnlySubpackageNames(fqn)
            .map { fqn.child(it) }

    override fun createInstanceOfLightScript(script: KtScript, module: KaModule?): KtLightClass? {
        if (module == null) return null
        return SymbolLightClassForScript(script, module)
    }

    override fun KtFile.findModule(): KaModule? = getModuleIfSupportEnabled()

    override fun declarationLocation(file: KtFile): DeclarationLocation? = when (file.getModuleIfSupportEnabled()) {
        is KaSourceModule -> DeclarationLocation.ProjectSources
        is KaLibraryModule -> DeclarationLocation.LibraryClasses
        is KaLibrarySourceModule -> DeclarationLocation.LibrarySources
        else -> null
    }

    override fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject, module: KaModule?): KtLightClass? {
        val lightClass = DecompiledLightClassesFactory.getLightClassForDecompiledClassOrObject(classOrObject, project)
        if (lightClass != null) {
            return lightClass
        }

        if (isMultiplatformSupportAvailable) {
            // Light classes for binary declarations are built over decompiled Java stubs which KMP files don't provide
            return createInstanceOfLightClass(classOrObject, module)
        }

        return null
    }

    @OptIn(KaImplementationDetail::class)
    override fun createInstanceOfLightClass(classOrObject: KtClassOrObject, module: KaModule?): KtLightClass? {
        val kaModule = module?.takeIf(KaModule::isLightClassSupportAvailable) ?: return null
        return createSymbolLightClassNoCache(classOrObject, kaModule)
    }

    override fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        val lightClass = DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeFqName, files)
        if (lightClass != null) {
            return lightClass
        }

        if (isMultiplatformSupportAvailable) {
            // Light classes for binary declarations are built over decompiled Java stubs which KMP files don't provide
            return createInstanceOfLightFacade(facadeFqName, files)
        }

        return null
    }

    override fun projectWideOutOfBlockModificationTracker(): ModificationTracker {
        return project.createProjectWideSourceModificationTracker()
    }

    override fun outOfBlockModificationTracker(element: PsiElement): ModificationTracker {
        return project.createProjectWideSourceModificationTracker()
    }

    override fun librariesTracker(element: PsiElement): ModificationTracker {
        return project.createProjectWideLibraryModificationTracker()
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        val module = files.first().getModuleIfSupportEnabled()
        if (module != null) {
            val lightClass = createInstanceOfLightFacade(facadeFqName, module, files)
            return lightClass
        }

        return null
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, module: KaModule, files: List<KtFile>): KtLightClassForFacade {
        return SymbolLightClassForFacade(facadeFqName, files, module)
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacadeByPackage(packageFqName, scope).toFacadeClasses(scope)
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacade(facadeFqName, scope).toFacadeClasses(scope)
    }

    @OptIn(KaImplementationDetail::class)
    private fun Collection<KtFile>.toFacadeClasses(scope: GlobalSearchScope): List<KtLightClassForFacade> = mapNotNull { file ->
        file.takeIf { it.facadeIsPossible() }?.let { file ->
            when (val response = KaLightClassesModuleHelper.getSuitableJvmModule(file, scope)) {
                is KaLightClassesModuleHelper.Response.MODULE_NOT_FOUND -> null
                is KaLightClassesModuleHelper.Response.MODULE_FOUND -> {
                    KaModuleConverter.getInstance(file.project)?.asKaModule(response.module)
                }
                else -> file.findModule()
            }
        }?.let { file to it }
    }.groupBy { (file, module) ->
        FacadeKey(file.javaFileFacadeFqName, file.isJvmMultifileClassFile, module)
    }.mapNotNull { (_, pairs) ->
        pairs.firstNotNullOfOrNull { (file, module) ->
            (file to module).takeIf { facadeIsApplicable(module, file) }
        }?.let { (file, module) -> getLightFacade(file, module) }
    }

    private fun getLightFacade(file: KtFile, module: KaModule): KtLightClassForFacade? = ifValid(file) {
        cacheLightClass(file, module) {
            createLightFacade(file, module)
        }
    }

    override val KaModule.contentSearchScope: GlobalSearchScope
        get() = GlobalSearchScope.union(
            buildList {
                add(contentScope)
                for (dependency in transitiveDependsOnDependencies) {
                    add(dependency.contentScope)
                }
            }
        )

    override fun facadeIsApplicable(module: KaModule, file: KtFile): Boolean =
        module.isFromSourceOrLibraryBinary() && module.isLightClassSupportAvailable()

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val facadeKtFiles = project.createDeclarationProvider(scope, null).findInternalFilesForFacade(fqName)
        if (facadeKtFiles.isEmpty()) return emptyList()

        val partShortName = fqName.shortName().asString()
        val partClassFileShortName = "$partShortName.class"

        return facadeKtFiles.mapNotNull { facadeKtFile ->
            if (facadeKtFile is KtClsFile) {
                val partClassFile = facadeKtFile.virtualFile.parent.findChild(partClassFileShortName) ?: return@mapNotNull null
                val psiFile = facadeKtFile.manager.findFile(partClassFile) as? KtClsFile ?: facadeKtFile
                val javaClsClass = DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(
                    mirrorFile = psiFile,
                    classFile = partClassFile,
                    correspondingClassOrObject = null,
                    project = project,
                ) ?: return@mapNotNull null

                KtLightClassForDecompiledDeclaration(javaClsClass, javaClsClass.parent, psiFile, null)
            } else {
                null
            }
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacade(facadeFqName)
    }

    override fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass = SymbolBasedFakeLightClass(classOrObject)

    private val lightClassCache = NestedCaffeineCache<KaModule, KtElement, KtLightClass>()

    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? = ifValid(classOrObject) {
        val kaModule = classOrObject.getModuleIfSupportEnabled() ?: return null
        cacheLightClass(classOrObject, kaModule) {
            createLightClass(classOrObject, kaModule)
        }
    }

    override fun getLightClass(classOrObject: KtClassOrObject, module: Module?): KtLightClass? = ifValid(classOrObject) {
        val kaModule = classOrObject.convertModuleOrDefaultIfSupportEnabled(module) ?: return null
        cacheLightClass(classOrObject, kaModule) {
            createLightClass(classOrObject, kaModule)
        }
    }

    override fun getLightClass(classOrObject: KtClassOrObject, module: KaModule?): KtLightClass? = ifValid(classOrObject) {
        if (module == null) return null
        cacheLightClass(classOrObject, module) {
            createLightClass(classOrObject, module)
        }
    }

    override fun getLightFacade(file: KtFile, module: Module?): KtLightClassForFacade? = ifValid(file) {
        val kaModule = file.convertModuleOrDefaultIfSupportEnabled(module) ?: return null
        cacheLightClass(file, kaModule) {
            createLightFacade(file, kaModule)
        }
    }

    override fun getLightFacade(file: KtFile): KtLightClassForFacade? = getLightFacade(file, null)

    @OptIn(KaImplementationDetail::class)
    override fun getLightClassForScript(script: KtScript, module: Module?): KtLightClass? = ifValid(script) {
        val kaModule = script.convertModuleOrDefaultIfSupportEnabled(module) ?: return null
        cacheLightClass(script, kaModule) {
            createLightScript(script, kaModule)
        }
    }

    private fun <R : KtLightClass> cacheLightClass(
        element: KtElement,
        module: KaModule,
        provider: () -> R?
    ): R? {
        @Suppress("UNCHECKED_CAST")
        return if (isMultiplatformSupportAvailable) {
            KMP_CACHE.get().computeIfAbsent(element) { provider() }
        } else {
            lightClassCache.getOrPut(module, element) { _, _ ->
                provider()
            }
        } as R?
    }

    private fun KtElement.isFromSourceOrLibraryBinary(): Boolean = getModuleIfSupportEnabled()?.isFromSourceOrLibraryBinary() == true

    private fun KaModule.isFromSourceOrLibraryBinary(): Boolean {
        return when (this) {
            is KaSourceModule -> true
            is KaLibraryModule -> true
            is KaDanglingFileModule -> contextModule.isFromSourceOrLibraryBinary()
            else -> false
        }
    }
}
