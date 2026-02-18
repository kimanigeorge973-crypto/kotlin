import org.gradle.kotlin.dsl.registering
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.konan.target.HostManager
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import plugins.publishing.configureMultiModuleMavenPublishing
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
}

val MODULE_NAME = "kotlin-power-assert-runtime"
fun KotlinCommonCompilerOptions.addReturnValueCheckerInfo() {
    freeCompilerArgs.add("-Xreturn-value-checker=full")
}

description = "Kotlin Power-Assert Runtime"
base.archivesName = MODULE_NAME

kotlin {
    explicitApi()

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    optIn.add("kotlin.contracts.ExperimentalContracts")
                }
            }
        }
    }

    metadata { // For common sources in IDE
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.addReturnValueCheckerInfo()
            }
        }
    }

    jvm {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    js {
        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            browser {}
        }
        nodejs {}
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.addAll(
                "-Xklib-ir-inliner=intra-module",
                "-Xir-module-name=$MODULE_NAME",
            )
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        (this as KotlinJsTargetDsl).compilerOptions {
            freeCompilerArgs.addAll(
                "-Xklib-ir-inliner=intra-module",
                "-source-map=false",
                "-source-map-embed-sources=",
            )
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$MODULE_NAME")
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        (this as KotlinJsTargetDsl).compilerOptions {
            freeCompilerArgs.addAll(
                "-Xklib-ir-inliner=intra-module",
                "-source-map=false",
                "-source-map-embed-sources=",
            )
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$MODULE_NAME")
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    if (!kotlinBuildProperties.isInIdeaSync.get()) {
        // Tier 1
        macosArm64()
        iosSimulatorArm64()
        iosArm64()

        // Tier 2
        linuxX64()
        linuxArm64()
        watchosSimulatorArm64()
        watchosArm32()
        watchosArm64()
        tvosSimulatorArm64()
        tvosArm64()

        // Tier 3
        androidNativeArm32()
        androidNativeArm64()
        androidNativeX86()
        androidNativeX64()
        mingwX64()
        watchosDeviceArm64()
        @Suppress("DEPRECATION") macosX64()
        @Suppress("DEPRECATION") iosX64()
        @Suppress("DEPRECATION") watchosX64()
        @Suppress("DEPRECATION") tvosX64()
    } else {
        // this magic is needed because of explicit dependency of common
        // source set on the stdlib
        when {
            HostManager.hostIsMac -> @Suppress("DEPRECATION") macosX64("native")
            HostManager.hostIsMingw -> mingwX64("native")
            HostManager.hostIsLinux -> linuxX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

tasks {
    val allMetadataJar by existing(Jar::class) {
        archiveClassifier = "all"
    }
    val sourcesJar by existing(Jar::class) {
        archiveAppendix = "metadata"
    }
    val jvmJar by existing(Jar::class) {
        archiveAppendix = null
        manifestAttributes(manifest, "Main")
    }
    val jvmSourcesJar by existing(Jar::class) {
        archiveAppendix = null
    }
    val jsJar by existing(Jar::class) {
        manifestAttributes(manifest, "Main")
        manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
    }
    val wasmJsJar by existing(Jar::class) {
        manifestAttributes(manifest, "Main")
        manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
    }
    val wasmWasiJar by existing(Jar::class) {
        manifestAttributes(manifest, "Main")
        manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
    }
}

configureDefaultPublishing()

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    val artifactBaseName = base.archivesName.get()
    configureMultiModuleMavenPublishing {
        val rootModule = module("rootModule") {
            mavenPublication {
                artifactId = artifactBaseName
                configureKotlinPomAttributes(project, description)
                artifact(emptyJavadocJar)
            }

            variant("metadataApiElements") { suppressPomMetadataWarnings() }
            variant("jvmApiElements")
            variant("jvmRuntimeElements") {
                configureVariantDetails { mapToMavenScope("runtime") }
            }
            variant("jvmSourcesElements")
            variant("nativeApiElements") {
                attributes {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("non-jvm"))
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                }
            }
        }

        val js = module("jsModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-js"
                configureKotlinPomAttributes(project, "$description for JS", packaging = "klib")
            }
            variant("jsApiElements")
            variant("jsRuntimeElements")
            variant("jsSourcesElements")
        }

        val wasmJs = module("wasmJsModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-wasm-js"
                configureKotlinPomAttributes(project, "$description for experimental WebAssembly JS platform", packaging = "klib")
            }
            variant("wasmJsApiElements")
            variant("wasmJsRuntimeElements")
            variant("wasmJsSourcesElements")
        }
        val wasmWasi = module("wasmWasiModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-wasm-wasi"
                configureKotlinPomAttributes(project, "$description for experimental WebAssembly WASI platform", packaging = "klib")
            }
            variant("wasmWasiApiElements")
            variant("wasmWasiRuntimeElements")
            variant("wasmWasiSourcesElements")
        }

        // Makes all variants from accompanying artifacts visible through `available-at`
        rootModule.include(js, wasmJs, wasmWasi)
    }

    publications {
        val rootModule by existing(MavenPublication::class)
        val jsModule by existing(MavenPublication::class)
        configureSbom("Main", MODULE_NAME, setOf("jvmRuntimeClasspath"), rootModule)
        configureSbom("Js", "$MODULE_NAME-js", setOf("jsRuntimeClasspath"), jsModule)

        val wasmJsModule by existing(MavenPublication::class)
        val wasmWasiModule by existing(MavenPublication::class)
        configureSbom("Wasm-Js", "$MODULE_NAME-wasm-js", setOf("wasmJsRuntimeClasspath"), wasmJsModule)
        configureSbom("Wasm-Wasi", "$MODULE_NAME-wasm-wasi", setOf("wasmWasiRuntimeClasspath"), wasmWasiModule)
    }
}
