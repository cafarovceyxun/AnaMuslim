package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.ic_bell_ring
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.mediaStillTooLarge
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementDaysLeft
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementDeleteConfirm
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementDuration
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementEmpty
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementExpired
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementHoursLeft
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementNeedsContent
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementNew
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementNoExpiry
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementNote
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementPublish
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementsSubtitle
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementsTitle
import com.cafarovceyxun.anamuslim.resources.storyDurationDay
import com.cafarovceyxun.anamuslim.resources.storyDurationForever
import com.cafarovceyxun.anamuslim.resources.storyDurationThreeDays
import com.cafarovceyxun.anamuslim.resources.storyDurationWeek
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.suggestionsAddMedia
import com.cafarovceyxun.anamuslim.resources.suggestionsImageFailed
import com.cafarovceyxun.anamuslim.resources.suggestionsImageUploading
import com.cafarovceyxun.anamuslim.resources.suggestionsMediaHint
import com.cafarovceyxun.anamuslim.resources.suggestionsMediaTooLarge
import com.cafarovceyxun.anamuslim.resources.suggestionsRemoveImage
import com.cafarovceyxun.anamuslim.resources.suggestionsVideoTooLong
import com.cafarovceyxun.anamuslim.resources.suggestionsViews
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.app.MediaPickResult
import com.cafarovceyxun.anamuslim.utils.app.rememberMediaPicker
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.IsoInstant
import com.cafarovceyxun.anamuslim.utils.supabase.StoryAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.StoryDuration
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.viewModels.StoryAnnouncementManagementViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Müstəqil hekayə («Elanlar») — təklifə və ya qəməri aya bağlı olmayan admin hekayəsi.
 *
 * Üstdə yeni hekayənin forması (media + mətn + görünmə müddəti), altda paylaşılmışlar: aktiv və
 * vaxtı bitmiş (sonuncular yalnız adminə görünür — RLS). Media seçici video/şəkil redaktorlarından
 * keçir ([rememberMediaPicker]), seçilən hər fayl dərhal yüklənir.
 */
@Composable
fun StoryAnnouncementManagementScreen() {
    val viewModel = viewModel { StoryAnnouncementManagementViewModel() }

    val stories by viewModel.stories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val draftMedia by viewModel.draftMedia.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    Scaffold(
        containerColor = colorScheme.background,
        topBar = { AppBar(title = stringResource(Res.string.storyAnnouncementsTitle)) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                // Edge-to-edge: klaviatura pəncərəni kiçiltmir — inset əl ilə (qəməri ekranı ilə eyni).
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = mainBottomNavigationOuterHeight() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "form") {
                    StoryForm(
                        draftMedia = draftMedia,
                        uploading = isUploading,
                        busy = isLoading,
                        onPickMedia = viewModel::addDraftMedia,
                        onRemoveMedia = viewModel::removeDraftMedia,
                        onDiscard = viewModel::discardDraft,
                        onPublish = { note, duration, onDone -> viewModel.publish(note, duration, onDone) },
                    )
                }

                if (stories.isEmpty() && !isLoading) {
                    item(key = "empty") {
                        MessageCard(
                            icon = Res.drawable.dr_icon_info,
                            message = stringResource(Res.string.storyAnnouncementEmpty),
                            style = MessageCardStyle.Info,
                        )
                    }
                }

                items(stories, key = { it.id }) { story ->
                    StoryCard(story = story, onDelete = { viewModel.delete(story) })
                }
            }

            if (isLoading && stories.isEmpty()) Loader(fill = true)
        }
    }
}

@Composable
private fun StoryForm(
    draftMedia: List<SuggestionMedia>,
    uploading: Boolean,
    busy: Boolean,
    onPickMedia: (com.cafarovceyxun.anamuslim.utils.app.PickedMedia) -> Unit,
    onRemoveMedia: (SuggestionMedia) -> Unit,
    onDiscard: () -> Unit,
    onPublish: (note: String?, duration: StoryDuration, onDone: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(StoryDuration.DAY) }

    val tooLongMsg = stringResource(Res.string.suggestionsVideoTooLong)
    val tooLargeMsg = stringResource(Res.string.suggestionsMediaTooLarge)
    val stillTooLargeMsg = stringResource(Res.string.mediaStillTooLarge)
    val failedMsg = stringResource(Res.string.suggestionsImageFailed)

    // Platformada seçici yoxdursa `null` — düymə görünmür (CLAUDE.md, «Provider/DI seam qaydası»).
    val pickMedia = rememberMediaPicker { result ->
        when (result) {
            is MediaPickResult.Picked -> onPickMedia(result.media)
            MediaPickResult.TooLong -> PlatformUtils.showLongToast(tooLongMsg)
            MediaPickResult.TooLarge -> PlatformUtils.showLongToast(tooLargeMsg)
            MediaPickResult.StillTooLarge -> PlatformUtils.showLongToast(stillTooLargeMsg)
            MediaPickResult.Failed -> PlatformUtils.showLongToast(failedMsg)
        }
    }

    val hasContent = draftMedia.isNotEmpty() || note.isNotBlank()

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
                painter = painterResource(Res.drawable.ic_bell_ring),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.storyAnnouncementNew),
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.storyAnnouncementsSubtitle),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                painter = painterResource(if (expanded) Res.drawable.dr_icon_close else Res.drawable.dr_icon_edit),
                contentDescription = stringResource(Res.string.storyAnnouncementNew),
                small = true,
                onClick = {
                    // Bağlamaq qaralamanı atır — yüklənmiş fayllar da silinir.
                    if (expanded) {
                        onDiscard()
                        note = ""
                        duration = StoryDuration.DAY
                    }
                    expanded = !expanded
                },
            )
        }

        if (!expanded) return@Column

        if (draftMedia.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                draftMedia.forEach { item ->
                    StoryMediaThumbnail(item = item, onRemove = { onRemoveMedia(item) })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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
            OutlinedButton(onClick = pickMedia, enabled = !busy) {
                Text(text = stringResource(Res.string.suggestionsAddMedia))
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.suggestionsMediaHint),
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        FormTextField(
            value = note,
            onValueChange = { note = it.take(NOTE_MAX) },
            label = stringResource(Res.string.storyAnnouncementNote),
            icon = Res.drawable.dr_icon_edit,
            minLines = 2,
            maxLines = 6,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.storyAnnouncementDuration),
            style = typography.labelLarge,
            color = colorScheme.onSurface,
        )

        Spacer(Modifier.height(6.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StoryDuration.entries.forEach { option ->
                Chip(
                    selected = duration == option,
                    label = { Text(text = stringResource(option.label())) },
                    onClick = { duration = option },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!hasContent) {
            Text(
                text = stringResource(Res.string.storyAnnouncementNeedsContent),
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                onPublish(note.trim(), duration) {
                    note = ""
                    duration = StoryDuration.DAY
                    expanded = false
                }
            },
            // Yükləmə bitməyibsə paylaşmaq yarımçıq hekayə yaradardı.
            enabled = hasContent && !busy && !uploading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.storyAnnouncementPublish))
        }
    }
}

@Composable
private fun StoryCard(story: StoryAnnouncement, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val now = currentEpochMillis()
    val active = story.isActive(now)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            // Vaxtı bitmiş hekayə istifadəçilərə görünmür — kartı da sönük göstəririk.
            .then(if (active) Modifier else Modifier.background(colorScheme.surface.alpha(0.4f))),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = listOfNotNull(
                    story.created_at?.let { IsoDate.display(it) },
                    expiryLabel(story, now),
                ).joinToString(" · "),
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (active) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            Icon(
                painter = painterResource(Res.drawable.dr_icon_eye),
                contentDescription = stringResource(Res.string.suggestionsViews),
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )

            Text(
                text = " ${story.view_count}",
                style = typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.width(6.dp))

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_delete),
                contentDescription = stringResource(Res.string.strLabelDelete),
                tint = colorScheme.error,
                small = true,
            ) { confirmDelete = true }
        }

        story.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(6.dp))

            Text(
                text = note,
                style = typography.bodySmall.withContentDirection(),
                color = colorScheme.onSurface.alpha(0.9f),
            )
        }

        if (story.media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                story.media.forEach { item -> StoryMediaThumbnail(item = item, onRemove = null) }
            }
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
                text = stringResource(Res.string.storyAnnouncementDeleteConfirm),
                style = typography.bodyMedium,
            )
        },
    )
}

/** «23 saat qalıb» / «2 gün qalıb» / «Vaxtı bitib» / «Müddətsiz» — saat qurşağı lazım deyil. */
@Composable
private fun expiryLabel(story: StoryAnnouncement, now: Long): String {
    val expiry = story.expires_at?.let(IsoInstant::toEpochMillis)
        ?: return stringResource(Res.string.storyAnnouncementNoExpiry)
    val left = expiry - now
    if (left <= 0) return stringResource(Res.string.storyAnnouncementExpired)

    val hours = ((left + 3_599_999L) / 3_600_000L).toInt()
    return if (hours <= 48) {
        stringResource(Res.string.storyAnnouncementHoursLeft, hours)
    } else {
        stringResource(Res.string.storyAnnouncementDaysLeft, (hours + 23) / 24)
    }
}

@Composable
private fun StoryMediaThumbnail(item: SuggestionMedia, onRemove: (() -> Unit)?) {
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
                painter = painterResource(if (item.isVideo) Res.drawable.ic_play else Res.drawable.dr_icon_feature),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        if (onRemove != null) {
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
}

private fun StoryDuration.label(): StringResource = when (this) {
    StoryDuration.DAY -> Res.string.storyDurationDay
    StoryDuration.THREE_DAYS -> Res.string.storyDurationThreeDays
    StoryDuration.WEEK -> Res.string.storyDurationWeek
    StoryDuration.FOREVER -> Res.string.storyDurationForever
}

/** `story_announcement_note_len` CHECK ilə eyni hədd. */
private const val NOTE_MAX = 1000
