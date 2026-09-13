plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kaleem.screenassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kaleem.screenassistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-bluff"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    // Bundled (not Play-Services-backed) model: ships in the APK, works fully
    // offline immediately, no download step, no Play Services requirement.
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
