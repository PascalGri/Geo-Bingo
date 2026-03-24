import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
        }
    }
    
    js {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val commonMain by getting
        val webMain by creating {
            dependsOn(commonMain)
        }
        jsMain.get().dependsOn(webMain)
        wasmJsMain.get().dependsOn(webMain)

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.play.services.location)
            implementation(libs.google.mobile.ads)
            implementation(libs.google.ump)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.datetime)
            implementation(compose.materialIconsExtended)
            implementation(libs.ktor.client.core)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "pg.geobingo.one"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pg.geobingo.one"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

tasks.matching { it.name == "embedAndSignAppleFrameworkForXcode" }.configureEach {
    doFirst {
        val srcRoot = System.getenv("SRCROOT") ?: return@doFirst
        val configuration = System.getenv("CONFIGURATION") ?: "Debug"
        val sdkName = System.getenv("SDK_NAME") ?: "iphoneos"
        // Clear xattrs from entire xcode-frameworks output dir (includes .dSYM)
        val xcodeFrameworksDir = "$srcRoot/../composeApp/build/xcode-frameworks/$configuration/$sdkName"
        ProcessBuilder("xattr", "-cr", xcodeFrameworksDir).start().waitFor()
        // Also clear from bin output (used by script fallback)
        val arch = System.getenv("ARCHS")?.split(" ")?.firstOrNull() ?: "iosArm64"
        val archForBin = if (arch == "arm64") "iosArm64" else "iosSimulatorArm64"
        val confLower = configuration.lowercase()
        val binFrameworkPath = "$srcRoot/../composeApp/build/bin/$archForBin/${confLower}Framework/ComposeApp.framework"
        ProcessBuilder("xattr", "-cr", binFrameworkPath).start().waitFor()
    }
}

// Desktop (JVM) target entfernt — kein Desktop-Release geplant

tasks.named<Copy>("jsProcessResources") {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
    from("src/webMain/resources")
}
tasks.named<Copy>("wasmJsProcessResources") {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
    from("src/webMain/resources")
}

// buildWeb: webpack-Bundle + statische Ressourcen in den Deploy-Output kopieren.
// Statt ./gradlew wasmJsBrowserProductionWebpack diesen Task verwenden.
tasks.register("buildWeb") {
    dependsOn("wasmJsBrowserProductionWebpack")
    doLast {
        val webpackDir = project.file("build/kotlin-webkit/wasmJs/productionExecutable").let {
            // Use the correct webpack output directory
            project.file("build/kotlin-webpack/wasmJs/productionExecutable")
        }
        val distDir = project.file("build/dist/wasmJs/productionExecutable")

        // Determine which WASM files the current composeApp.js actually references
        val composeAppJs = File(webpackDir, "composeApp.js")
        val referencedWasm: Set<String> = if (composeAppJs.exists()) {
            Regex("""[\w]{16,}\.wasm""").findAll(composeAppJs.readText()).map { it.value }.toSet()
        } else emptySet()

        // Clean dist to avoid stale files from previous builds
        project.delete(distDir)
        distDir.mkdirs()

        // Copy only currently referenced WASM files + JS bundle
        project.copy {
            from(webpackDir)
            into(distDir)
            include("composeApp.js", "composeApp.js.LICENSE.txt", "composeApp.js.map")
            if (referencedWasm.isNotEmpty()) include(referencedWasm) else include("*.wasm")
            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
        }
        // Copy static web resources (index.html, styles.css, etc.)
        project.copy {
            from("src/webMain/resources")
            into(distDir)
            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
        }
    }
}
