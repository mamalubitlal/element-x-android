import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = auto
}

// Webpack hashes the .wasm asset filenames, but the Kotlin/Wasm-generated
// mjs glue hardcodes `./chator-web-wasm-js.wasm` when fetching. After every
// distribution build, copy the WASM with the unhashed name so the bundle can
// find it. The Kotlin/Wasm file is much larger than the skiko runtime.
val renameWasmDist = tasks.register("renameWasmDist") {
    group = "build"
    description = "Copy the Kotlin/Wasm (larger) .wasm file to its unhashed name in dist"
    // Run after both dev and prod distributions. Use doLast only — no inputs
    // (Gradle 9 input validation would fail when prod dirs don't exist yet).
    doLast {
        val candidates = listOf(
            layout.buildDirectory.dir("kotlin-webpack/wasmJs/developmentExecutable").get().asFile,
            layout.buildDirectory.dir("kotlin-webpack/wasmJs/productionExecutable").get().asFile,
            layout.buildDirectory.dir("dist/wasmJs/developmentExecutable").get().asFile,
            layout.buildDirectory.dir("dist/wasmJs/productionExecutable").get().asFile,
        )
        for (dir in candidates) {
            if (!dir.exists()) continue
            val wasms = dir.listFiles { f -> f.name.endsWith(".wasm") } ?: continue
            val largest = wasms.maxByOrNull { it.length() } ?: continue
            val target = dir.resolve("chator-web-wasm-js.wasm")
            if (largest.absolutePath == target.absolutePath) continue
            if (target.exists() && target.length() == largest.length()) continue
            largest.copyTo(target, overwrite = true)
            logger.lifecycle("Copied ${largest.name} (${largest.length() / 1024} KB) -> chator-web-wasm-js.wasm in ${dir.name}")
        }
    }
}
tasks.matching { it.name.startsWith("wasmJsBrowser") && it.name.endsWith("Distribution") }.configureEach {
    finalizedBy(renameWasmDist)
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir("src/commonMain/kotlin")
kotlin.sourceSets.getByName("wasmJsMain").kotlin.srcDir("src/wasmJsMain/kotlin")
