/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.psi.*

abstract class KotlinAsJavaSupportBase<TModule : Any>(project: Project) : KotlinAsJavaSupportSharedBase<TModule>(project) {
    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? = ifValid(classOrObject) {
        cacheLightClass(classOrObject) {
            val lightClass = createLightClass(classOrObject, null)
            val containingFile = classOrObject.containingKtFile
            val cachedValue = when (declarationLocation(containingFile)) {
                DeclarationLocation.ProjectSources -> {
                    LightClassCachedValue(lightClass, outOfBlockModificationTracker(classOrObject))
                }
                DeclarationLocation.LibraryClasses, DeclarationLocation.LibrarySources -> {
                    LightClassCachedValue(lightClass, librariesTracker(classOrObject))
                }
                null -> if (containingFile.analysisContext != null || containingFile.originalFile.virtualFile != null) {
                    LightClassCachedValue(lightClass, outOfBlockModificationTracker(classOrObject))
                } else {
                    null
                }
            }

            cachedValueResult(cachedValue)
        }
    }

    override fun getLightClass(classOrObject: KtClassOrObject, module: Module?): KtLightClass? {
        return getLightClass(classOrObject)
    }

    override fun getLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass? {
        return getLightClass(classOrObject)
    }

    fun createLightFacade(file: KtFile): KtLightClassForFacade? {
        if (!file.facadeIsPossible()) {
            return null
        }

        val module = file.findModule()?.takeIf { facadeIsApplicable(it, file) } ?: return null
        return createLightFacade(file, module)
    }

    override fun getLightFacade(file: KtFile): KtLightClassForFacade? = ifValid(file) {
        cacheLightClass(file) {
            val lightFacade = createLightFacade(file) ?: return@cacheLightClass cachedValueResult(null)
            val facadeFiles = lightFacade.files
            val cachedValue = when {
                facadeFiles.none(KtFile::hasTopLevelCallables) -> null
                facadeFiles.none(KtFile::isCompiled) -> {
                    LightClassCachedValue(lightFacade, outOfBlockModificationTracker(file))
                }
                facadeFiles.all(KtFile::isCompiled) -> {
                    LightClassCachedValue(lightFacade, librariesTracker(file))
                }

                else -> error("Source and compiled files are mixed: $facadeFiles")
            }
            cachedValueResult(cachedValue)
        }
    }

    override fun getLightFacade(file: KtFile, module: Module?): KtLightClassForFacade? = getLightFacade(file)

    private fun <E : KtElement, R : KtLightClass> cacheLightClass(element: E, provider: CachedValueProvider<R>): R? {
        return CachedValuesManager.getCachedValue(element, provider)
    }

    override fun getLightClassForScript(script: KtScript, module: Module?): KtLightClass? = ifValid(script) {
        cacheLightClass(script) {
            val lightScript = createLightScript(script, null) ?: return@cacheLightClass cachedValueResult(null)
            val cachedValue = LightClassCachedValue(lightScript, projectWideOutOfBlockModificationTracker())
            cachedValueResult(cachedValue)
        }
    }

    private fun <T : KtLightClass> cachedValueResult(lightClassCachedValue: LightClassCachedValue<T>?): CachedValueProvider.Result<T> {
        val value = lightClassCachedValue?.value
        val tracker = lightClassCachedValue?.tracker ?: projectWideOutOfBlockModificationTracker()
        return CachedValueProvider.Result.createSingleDependency(value, tracker)
    }
}

class LightClassCachedValue<T : KtLightClass>(val value: T?, val tracker: ModificationTracker)
