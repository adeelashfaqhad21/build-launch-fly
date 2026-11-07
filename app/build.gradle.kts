plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.buildlaunchfly.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.buildlaunchfly.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

