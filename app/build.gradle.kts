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
    // Bundled, no network needed for detection itself: auto-detects the
    // source language from the OCR'd text.
    // OCR itself now runs on Tesseract (below), not ML Kit — ML Kit's Text
    // Recognition only supports Latin/Chinese/Devanagari/Japanese/Korean
    // script, no Arabic at any size, so it can't do the job here.
    implementation("com.google.mlkit:language-id:17.0.6")
    // On-device translation: downloads a small language-pack model once, then
    // every translation runs fully locally — no text or screenshot is ever
    // sent to a server for translation itself.
    implementation("com.google.mlkit:translate:17.0.3")
    // OCR engine. Unlike ML Kit, Tesseract supports Arabic (and ~100 other
    // languages) via a downloadable per-language trained-data file — see
    // TesseractOcr.kt for the download step.
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
}
