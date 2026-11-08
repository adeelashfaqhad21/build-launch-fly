plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Release signing. CI (or a local build) supplies the keystore through environment variables
 * or Gradle properties; without one the release APK falls back to the debug key so it is
 * still installable over `adb install`.
 */
val releaseStoreFile: String? =
    (project.findProperty("blfStoreFile") as String?) ?: System.getenv("BLF_STORE_FILE")
val releaseStorePassword: String? =
    (project.findProperty("blfStorePassword") as String?) ?: System.getenv("BLF_STORE_PASSWORD")
val releaseKeyAlias: String? =
    (project.findProperty("blfKeyAlias") as String?) ?: System.getenv("BLF_KEY_ALIAS")
val releaseKeyPassword: String? =
    (project.findProperty("blfKeyPassword") as String?) ?: System.getenv("BLF_KEY_PASSWORD")
val hasReleaseKey = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.buildlaunchfly.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.buildlaunchfly.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // An unsigned APK cannot be installed at all, so never produce one.
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += setOf("META-INF/*")
    }
}

dependencies {
    // Intentionally empty. The renderer is written directly against OpenGL ES 3.0 from the
    // Android framework (android.opengl.GLES30) and the Kotlin standard library comes with
    // the Kotlin Android plugin, so the build needs no third-party runtime dependency,
    // no NDK, no engine download and no asset pipeline.
}
