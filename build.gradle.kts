plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// AGP 9 built-in Kotlin defaults to KGP 2.2.10. Keep the whole build on Kotlin
// 2.4.20 (matches the Compose compiler plugin below) by declaring it on the
// top-level buildscript classpath as documented in the AGP 9.0 release notes.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}