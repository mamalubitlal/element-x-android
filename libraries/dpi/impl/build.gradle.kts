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
    // ByeByeDPI library for advanced DPI bypass (bundles native .so)
    implementation(projects.libraries.byedpi)

    testCommonDependencies(libs)
}
