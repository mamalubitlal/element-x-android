import extension.setupDependencyInjection
import extension.testCommonDependencies

/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-library")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.element.android.libraries.dpi.impl"

    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "x86", "arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments.addAll(listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_C_FLAGS=-std=c99",
                    "-DCMAKE_CXX_FLAGS=-std=c++17"
                ))
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            pickFirsts.addAll(listOf("**/libbyedpi.so"))
        }
    }
}

setupDependencyInjection()

dependencies {
    api(projects.libraries.dpi.api)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core)
    implementation(projects.appconfig)
    implementation(projects.libraries.di)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.core)
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
    // ByeByeDPI library for advanced DPI bypass
    implementation(project(":libraries:byedpi"))

    testCommonDependencies(libs)
}
