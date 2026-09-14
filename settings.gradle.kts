pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Tesseract4Android (Arabic-capable OCR) ships via JitPack, not
        // Maven Central.
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "ScreenAssistant"
include(":app")
