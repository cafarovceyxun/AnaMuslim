package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.api.ApiConfig
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateChecker
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.utils.supabase.AppRelease
import com.cafarovceyxun.anamuslim.utils.univ.rememberLargeFilePicker
import com.cafarovceyxun.anamuslim.viewModels.ApkUploadState
import com.cafarovceyxun.anamuslim.viewModels.AppReleaseAdminViewModel
import com.cafarovceyxun.anamuslim.viewModels.ReleaseSaveResult
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** Languages the release notes are authored in — the app's four locales. */
private val NOTE_LANGUAGES = listOf("az" to "Azərbaycanca", "en" to "İngiliscə", "tr" to "Türkcə", "ru" to "Rusca")

/**
 * `android` Play sətridir və elə qalmalıdır: `direct` build-dən əvvəlki **bütün** Android
 * quraşdırmaları (Play-dəkilər də, əl ilə qurulmuş köhnə APK-lar da) onu oxuyur. Yeni `direct` APK
 * isə öz sətrini (`android_apk`) oxuyur — ona görə APK-nı məcbur etmək Play-i məcbur etmir.
 */
private val PLATFORMS = listOf(
    "android" to "Google Play",
    AppUpdateChecker.CHANNEL_ANDROID_APK to "APK",
    "ios" to "App Store",
)

private fun isApk(platform: String) = platform == AppUpdateChecker.CHANNEL_ANDROID_APK

/**
 * Admin ekranı: mağazada hansı buraxılışın canlı olduğunu elan edir (`app_releases` cədvəli).
 *
 * Hər platforma öz sətrində saxlanılır, çünki nömrələr müqayisə oluna bilən deyil — Android-də
 * `versionCode`, iOS-də `CFBundleVersion`. Ana ekrandakı banner yalnız öz platformasının sətrini
 * oxuyur.
 */
@Composable
fun AppReleaseManagementScreen() {
    val vm = viewModel { AppReleaseAdminViewModel() }
    val releases by vm.releases.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isSaving by vm.isSaving.collectAsState()
    val lastResult by vm.lastResult.collectAsState()
    val apkState by vm.apkState.collectAsState()
    val apkExists by vm.apkExists.collectAsState()
    val apkCleanup by vm.apkCleanup.collectAsState()

    var platform by remember { mutableStateOf("android") }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            AppBar(
                title = "Buraxılış Bildirişi",
                actions = {
                    IconButton(painter = painterResource(Res.drawable.dr_icon_refresh)) { vm.load() }
                },
            )
        },
    ) { padding ->
        if (isLoading) {
            Loader(fill = true)
            return@Scaffold
        }

        val existing = releases.firstOrNull { it.platform == platform }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                // The bottom nav floats over this destination, so the scroll has to end above it —
                // otherwise the save button and the result note sit under the bar.
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = mainBottomNavigationOuterHeight() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PLATFORMS.forEach { (key, label) ->
                    FilterChip(
                        selected = platform == key,
                        onClick = {
                            platform = key
                            vm.clearResult()
                        },
                        label = { Text(label) },
                    )
                }
            }

            // `key = platform` so switching tabs rebuilds the form from the other row instead of
            // carrying the previous platform's numbers over into it.
            ReleaseForm(
                key = platform,
                platform = platform,
                existing = existing,
                isSaving = isSaving,
                onSave = { vm.save(it) },
                onEdited = { vm.clearResult() },
                apkSection = { versionCode ->
                    ApkSection(
                        versionCode = versionCode,
                        state = apkState,
                        exists = apkExists,
                        cleanup = apkCleanup,
                        onCheck = { vm.checkApk(it) },
                        onPicked = { file, code -> vm.uploadApk(file, code) },
                    )
                },
            )

            existing?.updated_at?.let {
                Text(
                    text = "Son dəyişiklik: ${it.substringBefore(".").replace("T", " ")}",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            when (lastResult) {
                ReleaseSaveResult.SAVED -> ResultNote("Yadda saxlanıldı — banner artıq canlıdır.", false)
                ReleaseSaveResult.BLOCKED -> ResultNote(
                    "Yazılmadı. Admin hesabı ilə giriş etdiyinizə əmin olun (RLS bloklayıb).",
                    true,
                )
                ReleaseSaveResult.INVALID -> ResultNote("Versiya nömrəsi 0-dan böyük olmalıdır.", true)
                null -> Unit
            }
        }
    }
}

@Composable
private fun ReleaseForm(
    key: String,
    platform: String,
    existing: AppRelease?,
    isSaving: Boolean,
    onSave: (AppRelease) -> Unit,
    onEdited: () -> Unit,
    apkSection: @Composable (versionCode: Long) -> Unit,
) {
    var latestVersion by remember(key) { mutableStateOf(existing?.latest_version?.takeIf { it > 0 }?.toString() ?: "") }
    var versionName by remember(key) { mutableStateOf(existing?.latest_version_name ?: "") }
    var minVersion by remember(key) { mutableStateOf(existing?.min_version?.toString() ?: "0") }
    var actionUrl by remember(key) {
        mutableStateOf(existing?.action_url ?: if (platform == "android") ApiConfig.PLAY_STORE_LISTING_URL else "")
    }
    // One note per line — the flattest editor that still round-trips a json array.
    val notes = remember(key) {
        NOTE_LANGUAGES.associate { (code, _) ->
            code to mutableStateOf(existing?.release_notes?.get(code)?.joinToString("\n") ?: "")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(
            value = latestVersion,
            onValueChange = { latestVersion = it.filter(Char::isDigit); onEdited() },
            label = when (platform) {
                "ios" -> "CFBundleVersion (iOS)"
                "android" -> "versionCode (Google Play)"
                else -> "versionCode (APK)"
            },
            numeric = true,
        )

        Field(
            value = versionName,
            onValueChange = { versionName = it; onEdited() },
            label = "Versiya adı (məs. 3.1.6)",
        )

        Field(
            value = minVersion,
            onValueChange = { minVersion = it.filter(Char::isDigit); onEdited() },
            label = "Minimum versiya — 0 = məcburi yeniləmə yoxdur",
            numeric = true,
        )

        Text(
            text = "Diqqət: minimum versiyanı sıfırdan böyük etsəniz, ondan aşağı bütün " +
                "quraşdırmalar açılışda bloklanır.",
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )

        Field(
            value = actionUrl,
            onValueChange = { actionUrl = it; onEdited() },
            label = if (isApk(platform)) "Link (boş qalsın — düymə APK-nı özü endirir)" else "Mağaza linki",
        )

        // APK yalnız öz tab-ında: Play və iOS sətirləri mağazaya göndərir.
        if (isApk(platform)) {
            apkSection(latestVersion.toLongOrNull() ?: 0L)
        } else if (platform == "android") {
            Text(
                text = "Bu sətri Play-dən quranlar və «APK» kanalından əvvəlki köhnə APK-lar oxuyur — " +
                    "minimum versiyanı qaldırsanız, hamısı Play Store-a yönləndirilir. Yeni APK " +
                    "quraşdırmaları buna baxmır, onlar «APK» tab-ındadır.",
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = "Yeniliklər — hər sətir bir bənd",
            style = typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
        )

        NOTE_LANGUAGES.forEach { (code, label) ->
            val state = notes.getValue(code)
            Field(
                value = state.value,
                onValueChange = { state.value = it; onEdited() },
                label = label,
                singleLine = false,
            )
        }

        Button(
            onClick = {
                val effectiveActionUrl = actionUrl.trim().ifBlank {
                    if (platform == "android") ApiConfig.PLAY_STORE_LISTING_URL else null
                }
                onSave(
                    AppRelease(
                        platform = platform,
                        latest_version = latestVersion.toLongOrNull() ?: 0L,
                        latest_version_name = versionName.trim().takeIf { it.isNotBlank() },
                        min_version = minVersion.toLongOrNull() ?: 0L,
                        action_url = effectiveActionUrl,
                        release_notes = notes.mapNotNull { (code, state) ->
                            val lines = state.value.lines().map(String::trim).filter(String::isNotEmpty)
                            if (lines.isEmpty()) null else code to lines
                        }.toMap(),
                    )
                )
            },
            enabled = !isSaving,
            shape = shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isSaving) "Göndərilir..." else "Yadda saxla və elan et")
        }
    }
}

@Composable
private fun ApkSection(
    versionCode: Long,
    state: ApkUploadState,
    exists: Boolean?,
    cleanup: String?,
    onCheck: (Long) -> Unit,
    onPicked: (com.cafarovceyxun.anamuslim.utils.univ.PickedLargeFile, Long) -> Unit,
) {
    // Nömrə rəqəm-rəqəm yazılır — hər basışda HEAD göndərməmək üçün qısa gecikmə.
    LaunchedEffect(versionCode) {
        delay(500)
        onCheck(versionCode)
    }

    val picker = rememberLargeFilePicker { picked ->
        if (picked != null) onPicked(picked, versionCode)
    }
    val uploading = state is ApkUploadState.Uploading

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        Text(
            text = "APK (mağazadan kənar yeniləmə)",
            style = typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
        )
        Text(
            text = "Play-dən qurmayanlar yeniləməni buradan çəkir. Fayl yuxarıdakı versionCode ilə " +
                "adlanır — əvvəl nömrəni yazın, sonra `direct` build-in APK-sını seçin.",
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )

        when (exists) {
            true -> Text("Bu versiya üçün APK yüklənib ✓", style = typography.bodySmall, color = colorScheme.primary)
            false -> Text("Bu versiya üçün APK yoxdur", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
            null -> Unit
        }

        OutlinedButton(
            onClick = { picker.pick() },
            enabled = !uploading && versionCode > 0,
            shape = shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("APK seç və yüklə")
        }

        // Boz düymə özü heç nə demir — fayl nömrə ilə adlandığı üçün əvvəl nömrə lazımdır.
        if (versionCode <= 0 && !uploading) {
            Text(
                text = "Düymə yuxarıda versionCode yazılandan sonra açılır.",
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        when (state) {
            is ApkUploadState.Uploading -> {
                LinearProgressIndicator(
                    progress = { state.fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Yüklənir… ${(state.fraction * 100).toInt()}%",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            ApkUploadState.Done -> ResultNote("APK yükləndi.", false)
            is ApkUploadState.Failed -> ResultNote(state.message, true)
            ApkUploadState.Idle -> Unit
        }

        cleanup?.let {
            Text(text = it, style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    numeric: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = typography.bodySmall) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ResultNote(text: String, isError: Boolean) {
    val color = if (isError) colorScheme.error else colorScheme.primary
    Text(
        text = text,
        style = typography.bodySmall,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}
