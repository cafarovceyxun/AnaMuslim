// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}

buildscript {
    extra["kotlin_version"] = "2.2.21"

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.kotlin.serialization)
        // KMP / Compose Multiplatform (iOS miqrasiyası üçün — yalnız :shared modulunda tətbiq olunur)
        classpath(libs.compose.gradle.plugin)
    }
}
