package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCardStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_lunar
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.lunarAnnouncementSubtitle
import com.cafarovceyxun.anamuslim.resources.lunarAnnouncementTitle
import com.cafarovceyxun.anamuslim.resources.lunarDeleteConfirm
import com.cafarovceyxun.anamuslim.resources.lunarEmpty
import com.cafarovceyxun.anamuslim.resources.lunarFieldHijriMonth
import com.cafarovceyxun.anamuslim.resources.lunarFieldHijriYear
import com.cafarovceyxun.anamuslim.resources.lunarFieldLength
import com.cafarovceyxun.anamuslim.resources.lunarFieldNote
import com.cafarovceyxun.anamuslim.resources.lunarFieldSighted
import com.cafarovceyxun.anamuslim.resources.lunarFieldStartDate
import com.cafarovceyxun.anamuslim.resources.lunarInvalidDate
import com.cafarovceyxun.anamuslim.resources.lunarLengthDays
import com.cafarovceyxun.anamuslim.resources.lunarPublish
import com.cafarovceyxun.anamuslim.resources.lunarStorySighted
import com.cafarovceyxun.anamuslim.resources.lunarStoryStart
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.suggestionsAddMedia
import com.cafarovceyxun.anamuslim.resources.suggestionsImageFailed
import com.cafarovceyxun.anamuslim.resources.suggestionsImageUploading
import com.cafarovceyxun.anamuslim.resources.suggestionsMediaHint
import com.cafarovceyxun.anamuslim.resources.suggestionsMediaTooLarge
import com.cafarovceyxun.anamuslim.resources.suggestionsRemoveImage
import com.cafarovceyxun.anamuslim.resources.suggestionsVideoTooLong
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.app.MediaPickResult
import com.cafarovceyxun.anamuslim.utils.app.rememberMediaPicker
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.hijriDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.LunarMonthLength
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.viewModels.LunarAnnouncementManagementViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Qəməri ay elanı — admin ayı gözlə görüb «bu ay filan tarixdə başladı, 29/30 gündür» deyir.
 *
 * Elan yayımlanan kimi **bütün telefonlarda** qəməri tarix ona uyğunlaşır və istifadəçinin öz
 * −2/+2 düzəlişi sıfırlanır ([com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences.applyLunarAnnouncement]).
 *
 * ⚠️ Media **ayrıca** qoşulur, forma ilə yox: ayın tarixi elan olunan kimi lazımdır, video isə
 * yüklənənə qədər saniyələr keçir. Ona görə əvvəl faktlar yayımlanır, video sonra sətrə əlavə
 * olunur — hekayə də elə həmin an canlanır.
 */
@Composable
fun LunarAnnouncementManagementScreen() {
    val viewModel = viewModel { LunarAnnouncementManagementViewModel() }

    val announcements by viewModel.announcements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadingFor by viewModel.uploadingFor.collectAsState()

    var editing by remember { mutableStateOf<LunarAnnouncement?>(null) }
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = { AppBar(title = stringResource(Res.string.lunarAnnouncementTitle)) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = mainBottomNavigationOuterHeight() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "form") {
                    AnnouncementForm(
                        // Redaktəyə açılanda sahələr həmin elandan dolur; `key` dəyişdiyi üçün
                        // formanın daxili vəziyyəti də yenidən qurulur.
                        initial = editing,
                        expanded = showForm || editing != null,
                        busy = isLoading,
                        onToggle = {
                            showForm = !showForm
                            if (!showForm) editing = null
                        },
                        onPublish = { year, month, start, length, sighted, note ->
                            viewModel.publish(year, month, start, length, sighted, note) {
                                showForm = false
                                editing = null
                            }
                        },
                    )
                }

                if (announcements.isEmpty() && !isLoading) {
                    item(key = "empty") {
                        MessageCard(
                            icon = Res.drawable.dr_icon_info,
                            message = stringResource(Res.string.lunarEmpty),
                            style = MessageCardStyle.Info,
                        )
                    }
                }

                items(announcements, key = { it.id }) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        uploading = uploadingFor == announcement.id,
                        onEdit = {
                            editing = announcement
                            showForm = true
                        },
                        onPickMedia = { viewModel.addMedia(announcement, it) },
                        onRemoveMedia = { viewModel.removeMedia(announcement, it) },
                        onDelete = { viewModel.delete(announcement) },
                    )
                }
            }

            if (isLoading && announcements.isEmpty()) Loader(fill = true)
        }
    }
}

/**
 * Elanın faktları. Qəməri ay/il **avtomatik təklif olunur**: admin ayı görəndə növbəti ayı elan
 * edir, ona görə default olaraq sabahın hicri ayı götürülür — səhv ay seçmək ən asan səhvdir.
 */
@Composable
private fun AnnouncementForm(
    initial: LunarAnnouncement?,
    expanded: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onPublish: (
        hijriYear: Int,
        hijriMonth: Int,
        startDate: String,
        lengthDays: Int,
        sightedAt: String?,
        note: String?,
    ) -> Unit,
) {
    val suggested = remember { suggestedMonth() }

    var month by remember(initial) {
        mutableStateOf((initial?.hijri_month ?: suggested?.first ?: 1).toString())
    }
    var year by remember(initial) {
        mutableStateOf((initial?.hijri_year ?: suggested?.second ?: 1447).toString())
    }
    var startDate by remember(initial) {
        mutableStateOf(initial?.start_date ?: currentLocalDateIsoString())
    }
    var length by remember(initial) {
        mutableStateOf(initial?.length_days ?: LunarMonthLength.LONG)
    }
    var sightedTime by remember(initial) {
        mutableStateOf(initial?.sighted_at?.substringAfter('T', "")?.take(5).orEmpty())
    }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }

    val dateValid = IsoDate.toEpochDay(startDate) != null
    val monthValue = month.toIntOrNull()
    val yearValue = year.toIntOrNull()
    val canPublish = dateValid &&
        monthValue in 1..12 &&
        yearValue != null &&
        yearValue in 1300..1700 &&
        !busy

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_lunar),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.lunarAnnouncementTitle),
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.lunarAnnouncementSubtitle),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                painter = painterResource(
                    if (expanded) Res.drawable.dr_icon_close else Res.drawable.dr_icon_edit
                ),
                contentDescription = stringResource(Res.string.lunarPublish),
                small = true,
                onClick = onToggle,
            )
        }

        if (!expanded) return@Column

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormTextField(
                value = month,
                onValueChange = { month = it.filter(Char::isDigit).take(2) },
                label = stringResource(Res.string.lunarFieldHijriMonth),
                icon = Res.drawable.dr_icon_lunar,
                keyboardType = KeyboardType.Number,
                error = monthValue !in 1..12,
                supportingText = monthValue
                    ?.takeIf { it in 1..12 }
                    ?.let { stringResource(PrayerUiFormat.hijriMonthName(it)) },
                modifier = Modifier.weight(1f),
            )

            FormTextField(
                value = year,
                onValueChange = { year = it.filter(Char::isDigit).take(4) },
                label = stringResource(Res.string.lunarFieldHijriYear),
                icon = Res.drawable.dr_icon_lunar,
                keyboardType = KeyboardType.Number,
                error = yearValue == null || yearValue !in 1300..1700,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        FormTextField(
            value = startDate,
            onValueChange = { startDate = it.take(10) },
            label = stringResource(Res.string.lunarFieldStartDate),
            placeholder = "2026-09-15",
            icon = Res.drawable.dr_icon_edit,
            error = !dateValid,
            errorText = stringResource(Res.string.lunarInvalidDate),
        )

        Spacer(Modifier.height(8.dp))

        // 29/30 — istifadəçinin gördüyü təqvimin sətir sayı da elə budur.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.lunarFieldLength),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )

            LunarMonthLength.ALL.forEach { value ->
                Chip(
                    selected = length == value,
                    label = { Text(text = stringResource(Res.string.lunarLengthDays, value)) },
                    onClick = { length = value },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Hilal ayın 1-i **başlamazdan əvvəlki axşam** görünür (qəməri gün gün batımında başlayır),
        // ona görə görünmə tarixi `start_date`-dən bir gün geridir. Nəticə sahənin altında yazılır
        // ki, admin başqa konvensiya işlədirsə dərhal görsün və tarixi əl ilə düzəltsin.
        val sightedDate = remember(startDate) { IsoDate.plusDays(startDate, -1) }

        FormTextField(
            value = sightedTime,
            onValueChange = { sightedTime = it.take(5) },
            label = stringResource(Res.string.lunarFieldSighted),
            placeholder = "19:42",
            icon = Res.drawable.dr_icon_info,
            supportingText = sightedDate?.let { IsoDate.display(it) },
        )

        Spacer(Modifier.height(8.dp))

        FormTextField(
            value = note,
            onValueChange = { note = it.take(300) },
            label = stringResource(Res.string.lunarFieldNote),
            icon = Res.drawable.dr_icon_feature,
            minLines = 2,
            maxLines = 4,
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                onPublish(
                    yearValue ?: return@Button,
                    monthValue ?: return@Button,
                    startDate,
                    length,
                    // Saat yazılmayıbsa `sighted_at` ümumiyyətlə göndərilmir — «00:00» yazmaq
                    // hekayədə «Ay göründü: 00:00» kimi yalan məlumat olardı.
                    //
                    // `+00:00` qəsdəndir: bu, ölçülmüş an deyil, **elan olunmuş** vaxtdır. Qurşaq
                    // çevirməsi tətbiq etsək eyni elan hər ölkədə başqa saat göstərərdi.
                    sightedTime.takeIf { it.length == 5 && sightedDate != null }
                        ?.let { "${sightedDate}T$it:00+00:00" },
                    note.trim().takeIf { it.isNotEmpty() },
                )
            },
            enabled = canPublish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }

            Text(text = stringResource(Res.string.lunarPublish))
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: LunarAnnouncement,
    uploading: Boolean,
    onEdit: () -> Unit,
    onPickMedia: (com.cafarovceyxun.anamuslim.utils.app.PickedMedia) -> Unit,
    onRemoveMedia: (SuggestionMedia) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    val tooLongMsg = stringResource(Res.string.suggestionsVideoTooLong)
    val tooLargeMsg = stringResource(Res.string.suggestionsMediaTooLarge)
    val failedMsg = stringResource(Res.string.suggestionsImageFailed)

    // Platformada seçici yoxdursa `null` gəlir və düymə ümumiyyətlə görünmür — basılıb heç nə
    // etməyən düymədən yaxşıdır (CLAUDE.md, «Provider/DI seam qaydası»).
    val pickMedia = rememberMediaPicker { result ->
        when (result) {
            is MediaPickResult.Picked -> onPickMedia(result.media)
            MediaPickResult.TooLong -> PlatformUtils.showLongToast(tooLongMsg)
            MediaPickResult.TooLarge -> PlatformUtils.showLongToast(tooLargeMsg)
            MediaPickResult.Failed -> PlatformUtils.showLongToast(failedMsg)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stringResource(PrayerUiFormat.hijriMonthName(announcement.hijri_month))} " +
                    "${announcement.hijri_year}",
                style = typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_edit),
                contentDescription = null,
                small = true,
                onClick = onEdit,
            )

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_delete),
                contentDescription = stringResource(Res.string.strLabelDelete),
                tint = colorScheme.error,
                small = true,
            ) { confirmDelete = true }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = listOfNotNull(
                stringResource(Res.string.lunarStoryStart, IsoDate.display(announcement.start_date)),
                stringResource(Res.string.lunarLengthDays, announcement.length_days),
                announcement.sighted_at
                    ?.substringAfter('T', "")
                    ?.take(5)
                    ?.takeIf { it.length == 5 }
                    ?.let { stringResource(Res.string.lunarStorySighted, it) },
            ).joinToString(" · "),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )

        announcement.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(6.dp))

            Text(
                text = note,
                style = typography.bodySmall.withContentDirection(),
                color = colorScheme.onSurface.alpha(0.9f),
            )
        }

        if (announcement.media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                announcement.media.forEach { item ->
                    LunarMediaThumbnail(item = item, onRemove = { onRemoveMedia(item) })
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (uploading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.suggestionsImageUploading),
                    style = typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        } else if (pickMedia != null) {
            OutlinedButton(onClick = pickMedia) {
                Text(text = stringResource(Res.string.suggestionsAddMedia))
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.suggestionsMediaHint),
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }

    AlertDialog(
        isOpen = confirmDelete,
        title = stringResource(Res.string.strLabelDelete),
        onClose = { confirmDelete = false },
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    confirmDelete = false
                    onDelete()
                },
            ),
        ),
        content = {
            Text(
                text = stringResource(Res.string.lunarDeleteConfirm),
                style = typography.bodyMedium,
            )
        },
    )
}

@Composable
private fun LunarMediaThumbnail(item: SuggestionMedia, onRemove: () -> Unit) {
    val preview = if (item.isVideo) null else rememberRemoteImage(item.url)

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(
                    if (item.isVideo) Res.drawable.ic_play else Res.drawable.dr_icon_lunar
                ),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        // Ortaq IconButton `modifier` qəbul etmir, ona görə yerləşdirmə Box-dadır.
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
            IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.suggestionsRemoveImage),
                tint = colorScheme.onError,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.error,
                    contentColor = colorScheme.onError,
                ),
                small = true,
            ) { onRemove() }
        }
    }
}

/**
 * Formanın default qəməri ayı — **sabahın** ayı.
 *
 * Ay axşam görünür və elan növbəti gün başlayan ay üçündür; bugünkü ayı təklif etsəydik admin
 * hər dəfə ayı əl ilə bir irəli sürməli olardı, unudanda isə elan **keçmiş aya** yazılardı.
 */
private fun suggestedMonth(): Pair<Int, Int>? {
    val tomorrow = currentEpochMillis() + 86_400_000L

    return hijriDate(tomorrow)?.let { (_, month, year) -> month to year }
}
