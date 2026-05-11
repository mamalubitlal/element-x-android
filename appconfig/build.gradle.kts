import config.BuildTimeConfig
import extension.buildConfigFieldStr
import java.util.Properties

/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-library")
}

// Load chator config from properties file
val chatorConfigFile = rootProject.file("chator-config.properties")
val chatorConfig = Properties()
if (chatorConfigFile.exists()) {
    chatorConfig.load(chatorConfigFile.inputStream())
}

// Default values
val defaultHomeServer = chatorConfig.getProperty("CHATOR_HOMESERVER_URL", "https://matrix.org")
val defaultAppName = chatorConfig.getProperty("CHATOR_APP_NAME", "чатор")
val defaultServerName = chatorConfig.getProperty("CHATOR_SERVER_NAME", "matrix.org")
val defaultDebug = chatorConfig.getProperty("CHATOR_DEBUG", "false").toBoolean()
val defaultTheme = chatorConfig.getProperty("CHATOR_DEFAULT_THEME", "system")

android {
    namespace = "io.element.android.appconfig"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigFieldStr(
            name = "DEFAULT_MATRIX_URL",
            value = "\"$defaultHomeServer\"",
        )
        buildConfigFieldStr(
            name = "APP_NAME",
            value = "\"$defaultAppName\"",
        )
        buildConfigFieldStr(
            name = "SERVER_NAME",
            value = "\"$defaultServerName\"",
        )
        buildConfigField(
            name = "DEBUG_MODE",
            value = defaultDebug,
        )
        buildConfigFieldStr(
            name = "DEFAULT_THEME",
            value = "\"$defaultTheme\"",
        )
        buildConfigFieldStr(
            name = "URL_POLICY",
            value = if (isEnterpriseBuild) {
                BuildTimeConfig.URL_POLICY ?: ""
            } else {
                "https://element.io/cookie-policy"
            },
        )
        buildConfigFieldStr(
            name = "BUG_REPORT_URL",
            value = if (isEnterpriseBuild) {
                BuildTimeConfig.BUG_REPORT_URL ?: ""
            } else {
                "https://rageshakes.element.io/api/submit"
            },
        )
        buildConfigFieldStr(
            name = "BUG_REPORT_APP_NAME",
            value = if (isEnterpriseBuild) {
                BuildTimeConfig.BUG_REPORT_APP_NAME ?: ""
            } else {
                "element-x-android"
            },
        )
    }
}

dependencies {
    implementation(libs.androidx.annotationjvm)
    implementation(projects.libraries.matrix.api)
}
