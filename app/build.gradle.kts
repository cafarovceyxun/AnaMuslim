import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
}


// Room schema export (used for migration tests / history). KSP is incremental by default,
// so the former kapt-only `room.incremental` option is dropped.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Migrated from the deprecated `android { kotlinOptions { } }` block (removed as a hard error in
// Kotlin 2.3 with AGP 9): the compiler options now live in the top-level `kotlin` extension.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }
}

// Release signing is read from `keystore.properties` (kept OUT of the repo — see
// .gitignore). If the file is absent (CI, forks, contributors), release builds fall
// back to unsigned so the project still builds. Provide the file locally, or inject
// the four keys as CI secrets, to produce a signed release.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

/** Set only when building the self-update test copy of `direct` (see that build type). */
val apkTestVersionCode: Int? = providers.gradleProperty("apkTestVersionCode").orNull?.toInt()

android {
    namespace = "com.cafarovceyxun.anamuslim"
    compileSdk = 36

    defaultConfig {
        // Production applicationId — must match the Play Store listing. Debug builds get a
        // `.test` suffix (see the debug buildType) so a test build installs alongside the
        // released app instead of replacing it, without ever risking a wrong package on Play.
        applicationId = "com.cafarovceyxun.anamuslim"
        minSdk = 24
        targetSdk = 36

        versionCode = 202611151
        versionName = "2026.11.15"

        resValue("string", "app_name", "Ənə Muslim")

        // Whether this build may download and install its own update. Off everywhere except the
        // `direct` build type below — Play forbids a Play-distributed app from updating itself.
        buildConfigField("boolean", "SELF_UPDATE", "false")
        // Which `app_releases` row this build reads for its update banner and forced update.
        // `android` is the row every installed build before the `direct` type has always read —
        // the Play installs among them — so it stays the Play channel; only `direct` reads its own.
        buildConfigField("String", "UPDATE_PLATFORM", "\"android\"")

        // Yalnız ARM: sherpa-onnx hər ABI üçün 17–33 MB-lıq `.so` gətirir və x86/x86_64 nüsxələri
        // universal APK-nı 131 MB-a qaldırmışdı (62 MB-ı yalnız emulyator/x86 Chromebook üçün).
        // Apple Silicon emulyatoru arm64-dür; x86 emulyator lazım olsa bu sətri müvəqqəti genişlət.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner ="androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.generatedDensities()
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false

            // Installs as com.cafarovceyxun.anamuslim.test — side by side with the released app,
            // so testing on a device never replaces the production install.
            applicationIdSuffix = ".test"

            resValue("string", "app_name", "Ənə Muslim(test)")

            /* ---------------------------------------------------------------- */
            resValue("string", "cleartextTrafficPermitted", "true")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            /* ---------------------------------------------------------------- */
            resValue("string", "cleartextTrafficPermitted", "false")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        // The APK handed out outside Play. Identical to `release` (same package, same key, same
        // shrinking) plus one thing: it updates itself in place (`utils/app/SelfUpdater.kt`). The
        // install permissions that needs live in `src/direct/AndroidManifest.xml`, so the Play
        // build never declares them. Build: `./gradlew :app:assembleDirect`.
        create("direct") {
            initWith(getByName("release"))
            // `:shared` only has debug/release.
            matchingFallbacks += "release"

            buildConfigField("boolean", "SELF_UPDATE", "true")
            // Its own row, so forcing an APK update never forces Play installs (and back).
            buildConfigField("String", "UPDATE_PLATFORM", "\"android_apk\"")

            // Self-update test copy: `-PapkTestVersionCode=<n>` turns this into a separate app
            // (`.apktest`, own name) that installs beside the Play app and the debug build, so the
            // whole download → install path can be tried on a real phone without touching either.
            // Build an "old" and a "new" one with two numbers; see `apkTestVersionCode` below.
            if (apkTestVersionCode != null) {
                applicationIdSuffix = ".apktest"
                versionNameSuffix = "-apktest"
                resValue("string", "app_name", "Ənə Muslim (APK test)")
            }
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

androidComponents {
    onVariants(selector().withBuildType("direct")) { variant ->
        apkTestVersionCode?.let { code -> variant.outputs.forEach { it.versionCode.set(code) } }
    }
}

base {
    archivesName = android.defaultConfig.versionName
}

/**
 * sherpa-onnx — hədisləri cihazda səsləndirən mühərrik (öz Piper modelimiz, `utils/speech/`).
 *
 * Maven-də rəsmi paket yoxdur, AAR yalnız GitHub buraxılışındadır; 39 MB-lıq binarı repoya qoymamaq
 * üçün ilk konfiqurasiyada endirilir və sha256 ilə yoxlanılır (`third_party/` git-ə düşmür).
 *
 * ⚠️ **1.13.8-də sabitdir:** 2.0 espeak-ng-ni kitabxanadan çıxarıb, bizim Piper modeli isə fonemləri
 * məhz ondan alır. iOS-dakı `SherpaOnnxC.xcframework` da eyni versiyadır
 * (`iosApp/ci_scripts/fetch_sherpa_onnx.sh`) — ikisini birlikdə yenilə.
 * «static-link-onnxruntime» variantı: onnxruntime ayrıca `.so` kimi gəlmir, APK daha kiçikdir.
 */
val sherpaOnnxVersion = "1.13.8"
val sherpaOnnxSha256 = "b22c3fc1b6a45666d28892bb2f7694beeb77a8362d7ebd77c1a5431ec9435471"
val sherpaOnnxAar = rootProject.file(
    "third_party/sherpa-onnx/sherpa-onnx-static-link-onnxruntime-$sherpaOnnxVersion.aar"
)

fun sha256Of(file: File): String = MessageDigest.getInstance("SHA-256").run {
    file.inputStream().use { input ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            update(buffer, 0, read)
        }
    }
    digest().joinToString("") { "%02x".format(it) }
}

if (!sherpaOnnxAar.exists()) {
    val url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$sherpaOnnxVersion/" +
        "sherpa-onnx-static-link-onnxruntime-$sherpaOnnxVersion.aar"
    logger.lifecycle("sherpa-onnx $sherpaOnnxVersion endirilir: $url")
    sherpaOnnxAar.parentFile.mkdirs()
    val part = File(sherpaOnnxAar.path + ".part")
    uri(url).toURL().openStream().use { input -> part.outputStream().use { input.copyTo(it) } }
    val actual = sha256Of(part)
    if (actual != sherpaOnnxSha256) {
        part.delete()
        throw GradleException("sherpa-onnx AAR sha256 uyğun gəlmir: $actual")
    }
    part.renameTo(sherpaOnnxAar)
}

dependencies {
    implementation(files(sherpaOnnxAar))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":shared"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowSizeClass)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.ui.tooling)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.androidx.coreKtx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.service)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.legacySupport)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.media)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activityKtx)
    implementation(libs.androidx.activityCompose)
    implementation(libs.splashscreen)
    implementation(libs.androidx.fragmentKtx)

    implementation(libs.media3ExoPlayer)
    implementation(libs.media3Datasource)
    implementation(libs.media3Database)
    implementation(libs.media3Session)
    implementation(libs.media3UI)

    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.material)
    implementation(libs.apache.commons)
    implementation(libs.guava)
    implementation(libs.viewbinding)

    /* kotlinx serialization */
    implementation(libs.kotlinxSerialization)

    implementation(libs.workManager)
    implementation(libs.dataStore)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.paging)
    implementation(libs.pagingCompose)
    implementation(libs.roomPaging)


    implementation(libs.compose.navigation)

    implementation(libs.glance)
    implementation(libs.glance.appwidget)

}
