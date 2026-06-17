# App Icon Setup Investigation - Element X Android

## 1. appicon/chator/build.gradle.kts
```kotlin
/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.appicon.chator"

    buildTypes {
        register("nightly")
    }
}
```

## 2. app/build.gradle.kts (lines 250-270)
```kotlin
     val reportingExtension: ReportingExtension = project.extensions.getByType(ReportingExtension::class.java)
     configureLicensesTasks(reportingExtension)
 }

 setupDependencyInjection()

 dependencies {
     allLibrariesImpl()
     allServicesImpl()
     if (isEnterpriseBuild) {
         allEnterpriseImpl(project)
         implementation(projects.appicon.enterprise)
     } else {
         allEnterpriseImpl(project)
         implementation(projects.appicon.chator)
     }
     allFeaturesImpl(project)
     implementation(projects.features.migration.api)
     implementation(projects.appnav)
     implementation(projects.appconfig)
     implementation(projects.libraries.uiStrings)
     implementation(projects.services.analytics.compose)
```

## 3. appicon/element/build.gradle.kts
```kotlin
/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.appicon.element"

    buildTypes {
        register("nightly")
    }
}
```

## 4. appicon/chator/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background_chator"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground_chator"/>
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome_chator"/>
</adaptive-icon>
```

## 5. appicon/chator/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background_chator"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground_chator"/>
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome_chator"/>
</adaptive-icon>
```

## 6. app/src/main/AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- To be able to install APK from the application -->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

    <application
        android:name=".ElementXApplication"
        android:allowBackup="false"
        android:appCategory="social"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:enableOnBackInvokedCallback="true"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:largeHeap="true"
        android:localeConfig="@xml/locales_config"
        android:networkSecurityConfig="@xml/network_security_config"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ElementX"
        tools:targetApi="33">

        <!-- ... rest of manifest ... -->

    </application>

</manifest>
```

## 7. Other ic_launcher*.xml files found
- `C:\chtor\appicon\enterprise\src\main\res\mipmap-anydpi\ic_launcher.xml`
- `C:\chtor\appicon\enterprise\src\main\res\mipmap-anydpi\ic_launcher_round.xml`
- `C:\chtor\appicon\element\src\main\res\mipmap-anydpi-v26\ic_launcher.xml`
- `C:\chtor\appicon\element\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml`
- `C:\chtor\appicon\chator\src\main\res\mipmap-anydpi-v26\ic_launcher.xml`
- `C:\chtor\appicon\chator\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml`

## Key Findings

### Icon Module Selection Logic
The app uses dynamic icon modules based on build type:
- **Enterprise builds**: Use `appicon.enterprise` module
- **Non-enterprise builds**: Use `appicon.chator` module (along with enterprise foss features)

### Icon Resource Structure
- **Chator/Element modules** (v26 adaptive icons with monochrome support):
  - Background: `@drawable/ic_launcher_background_{chator|element}`
  - Foreground: `@mipmap/ic_launcher_foreground_{chator|element}`
  - Monochrome: `@mipmap/ic_launcher_monochrome_{chator|element}` (Android 8.0+)
  
- **Enterprise module** (legacy adaptive icons):
  - Background: `@drawable/ic_launcher_background_enterprise`
  - Foreground: `@mipmap/ic_launcher_foreground_enterprise`
  - No monochrome support (uses legacy mipmap-anydpi without v26 qualifier)

### Manifest References
The AndroidManifest.xml references generic icon names that resolve based on the build variant:
- `android:icon="@mipmap/ic_launcher"`
- `android:roundIcon="@mipmap/ic_launcher_round"`

These resolve to the appropriate module-specific resources during the build process.

### Module Competition
The `appicon/element` and `appicon/chator` modules are structurally identical except for resource name suffixes (`element_*` vs `chator_*`). The selection between them appears to be controlled by the build configuration logic in the app's dependencies block, where non-enterprise builds include the chator module.