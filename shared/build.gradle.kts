plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.ksp)
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    // iosX64 (Intel-Mac simulator) is deliberately absent: androidx.paging 3.4.2 — like a growing
    // number of androidx KMP artifacts — publishes only iosArm64 + iosSimulatorArm64, and Gradle
    // fails the whole commonMain source set if one target cannot resolve. No end-user impact:
    // real devices are iosArm64 and Apple-Silicon simulators are iosSimulatorArm64. Re-adding it
    // means giving up every androidx KMP dependency that skips it.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            // Common BackHandler (androidx.compose.ui.backhandler); not transitive on Android.
            implementation(libs.compose.ui.backhandler)
            // api (not implementation): the generated `Res`/`DrawableResource`/`StringResource`
            // surface is referenced directly by app-module callers (e.g. MessageCard(icon = Res.drawable.x)),
            // so it must be exposed transitively.
            api(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinxSerialization)
            implementation(libs.kotlinxCoroutines.core)
            // Ktor core exposed as api so app callers can reference ByteReadChannel / streaming types.
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            // Room KMP + bundled SQLite driver (native iOS + Android)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            // Multiplatform file I/O (shared AppFileSystem)
            implementation(libs.okio)
            // DataStore KMP
            implementation(libs.dataStore)
            // KMP core ViewModel (androidx.lifecycle.ViewModel + viewModelScope) for shared view models.
            api(libs.androidx.lifecycle.viewmodel)
            // Compose `viewModel<T>()` helper so shared composables can obtain shared view models
            // directly (first consumer: LoginSheet → AuthViewModel). Uses the JetBrains multiplatform
            // variant because androidx.lifecycle:lifecycle-viewmodel-compose has no iOS publication.
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            // `collectAsStateWithLifecycle`, already used by shared screens. It arrives transitively
            // via lifecycle-viewmodel-compose above; declared explicitly because commonMain imports it
            // directly, and a transitive-only path is what bit us with compose-ui-backhandler.
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            // Compose MP navigation. `api` (not implementation): shared screens expose NavHostController
            // in their signatures, and the app module's Activities host the shared NavHost.
            // JetBrains variant — androidx.navigation:navigation-compose has no iOS publication.
            api(libs.jetbrains.navigation.compose)
            // Supabase KMP (Postgrest + Auth). Exposed as `api` because several app-module
            // callers (viewmodels, workers) still call the `io.github.jan.supabase.*` extension
            // functions (`.from(...)`, `.auth`, `.postgrest`) directly on SupabaseProvider.client
            // -- mirrors the `api(libs.ktor.client.core)` seam above. The version-pinning BOM is
            // added separately below via `add("commonMainApi", platform(...))`: calling the
            // top-level `platform()` helper directly inside this typed KMP dependencies block
            // hits a deprecated/ambiguous overload (KT-58759).
            // Paging 3 (KMP). `api`, not `implementation`: shared view models expose
            // `Flow<PagingData<T>>` in their signatures and app screens collect it, so the app
            // module must see these types. paging-common carries PagingSource/Pager/PagingData;
            // paging-compose carries `collectAsLazyPagingItems`. Both publish iosArm64 +
            // iosSimulatorArm64 at 3.4.2 (verified via iosSimulatorArm64CompileKlibraries).
            api(libs.pagingCommon)
            api(libs.pagingCompose)
            api(libs.supabase.postgrest)
            api(libs.supabase.auth)
        }
        androidMain.dependencies {
            implementation(libs.androidx.coreKtx)
            implementation(libs.androidx.appcompat)
            // activity-compose provides LocalOnBackPressedDispatcherOwner for the Android actual of
            // rememberSystemBack() (shared AppBar/BackButton default back action).
            implementation(libs.androidx.activityCompose)
            // ui-text 1.10 for LineHeightStyle.Mode.Tight (tightTextStyle Android actual); the app
            // already resolves this version, so nothing changes at runtime.
            implementation(libs.compose.ui.text)
            implementation(libs.ktor.client.okhttp)
            // Hekayə videosu üçün (StoryVideo.android.kt). Versiya `:app`-dakı ilə eyni ref-dir,
            // ona görə yeni versiya gətirmir — sadəcə başqa source set-də görünür.
            implementation(libs.media3ExoPlayer)
            implementation(libs.media3UI)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Custom back input for the Mac build (see MacBack.kt); transitive through
            // compose-ui-backhandler, declared explicitly because iosMain compiles against it.
            implementation(libs.androidx.navigationevent)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        }
    }
}

android {
    namespace = "com.cafarovceyxun.anamuslim.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Compose Multiplatform resources (`Res.string`/`Res.drawable`), migrated from the app module's
// Android res/. A stable, explicit package keeps the generated accessors clean as UI moves over.
compose.resources {
    publicResClass = true
    packageOfResClass = "com.cafarovceyxun.anamuslim.resources"
}

// Forward the real pre-packaged Quran DB path into native simulator tests
// (simctl only passes env vars carrying the SIMCTL_CHILD_ prefix to the spawned process).
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    environment(
        "SIMCTL_CHILD_QURANAPP_DB_ASSET",
        rootProject.file("app/src/main/assets/db/quranapp.db").absolutePath,
    )
}

// Room KSP processor must run per target (Android + each iOS target).
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)

    // Supabase BOM, pinning postgrest-kt/auth-kt (declared as `api` in commonMain above) for
    // every source set. Added here (not inside `kotlin { sourceSets { commonMain.dependencies
    // {} } }`) because the KMP-scoped `platform()` overload is deprecated/ambiguous (KT-58759);
    // this classic top-level `dependencies {}` block uses the unambiguous Gradle one.
    add("commonMainApi", platform(libs.supabase.bom))
}
