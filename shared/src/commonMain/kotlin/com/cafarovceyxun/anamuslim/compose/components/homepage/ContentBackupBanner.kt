package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.contentBackupDaysAgo
import com.cafarovceyxun.anamuslim.resources.contentBackupFailed
import com.cafarovceyxun.anamuslim.resources.contentBackupNever
import com.cafarovceyxun.anamuslim.resources.contentBackupRunning
import com.cafarovceyxun.anamuslim.resources.contentBackupSaved
import com.cafarovceyxun.anamuslim.resources.contentBackupTake
import com.cafarovceyxun.anamuslim.resources.contentBackupTitle
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.univ.rememberTextDocumentSaver
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.ContentBackupStatus
import com.cafarovceyxun.anamuslim.viewModels.ContentBackupViewModel
import org.jetbrains.compose.resources.stringResource

/** Bu qədər gündən sonra ana ekranda xatırladılır. */
private const val REMIND_AFTER_DAYS = 3
private const val DAY_MILLIS = 86_400_000L

/** Ekranların işlətdiyi hazır dəst: vəziyyət + «yedək al» əməliyyatı. */
@Stable
class ContentBackupUi(
    val lastBackupAt: Long,
    val isRunning: Boolean,
    val start: () -> Unit,
) {
    val daysSinceBackup: Int?
        get() = if (lastBackupAt <= 0L) null else ((currentEpochMillis() - lastBackupAt) / DAY_MILLIS).toInt()

    val isDue: Boolean
        get() = daysSinceBackup?.let { it >= REMIND_AFTER_DAYS } ?: true
}

/**
 * Yedəyi qurur və sistem «hara saxlayım?» seçicisini açır.
 *
 * Saxlayıcı **kompozisiya ömürlüdür** ([rememberTextDocumentSaver] — Android nəticəni yalnız
 * `ActivityResultLauncher` ilə verir), ağır iş isə ViewModel-dədir; ikisi [LaunchedEffect] ilə
 * bağlanır: fayl hazır olanda seçici açılır.
 */
@Composable
fun rememberContentBackup(): ContentBackupUi {
    val viewModel = viewModel { ContentBackupViewModel() }
    val status by viewModel.status.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()

    val savedMessage = stringResource(Res.string.contentBackupSaved)
    val failedMessage = stringResource(Res.string.contentBackupFailed)

    val saver = rememberTextDocumentSaver { saved ->
        PlatformUtils.showLongToast(if (saved) savedMessage else failedMessage)
        viewModel.onSaved(saved)
    }

    LaunchedEffect(status) {
        when (val current = status) {
            is ContentBackupStatus.Ready -> {
                // Sıra vacibdir: əvvəlcə tarix yazılır (proses öldürülsə də qalsın), sonra seçici.
                viewModel.onSaveLaunched()
                saver.save(current.fileName, current.json)
            }
            ContentBackupStatus.Failed -> {
                PlatformUtils.showLongToast(failedMessage)
                viewModel.onErrorShown()
            }
            else -> Unit
        }
    }

    return remember(lastBackupAt, status) {
        ContentBackupUi(
            lastBackupAt = lastBackupAt,
            // Yazma bitənə qədər banner kompozisiyada qalmalıdır: fayl seçicisinin nəticəsini
            // burada qeydiyyatdan keçmiş launcher gətirir, banner çıxsa nəticə düşər.
            isRunning = status != ContentBackupStatus.Idle && status != ContentBackupStatus.Failed,
            start = viewModel::start,
        )
    }
}

/**
 * Ana ekranın yuxarısında, **yalnız admin girişi olanda** və son yedəkdən
 * [REMIND_AFTER_DAYS] gün keçəndə görünən xatırlatma.
 *
 * Niyə banner, niyə sistem bildirişi yox: yedək yalnız adminin özünə lazımdır, tətbiq isə
 * ictimaidir — planlaşdırılmış bildiriş hər cihazda qurulub yalnız bir nəfər üçün işləyərdi.
 */
@Composable
fun ContentBackupBanner() {
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsStateWithLifecycle()

    if (session == null) return

    val backup = rememberContentBackup()
    if (!backup.isDue && !backup.isRunning) return

    val days = backup.daysSinceBackup

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shapes.large)
            .background(colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.contentBackupTitle),
                style = typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = if (days == null) {
                    stringResource(Res.string.contentBackupNever)
                } else {
                    stringResource(Res.string.contentBackupDaysAgo, days)
                },
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        if (backup.isRunning) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Button(onClick = backup.start) {
                Text(stringResource(Res.string.contentBackupTake))
            }
        }
    }
}

/** Ekran mətnini bir yerdə saxlamaq üçün: admin bölməsindəki bənd də eyni sətirləri işlədir. */
@Composable
fun contentBackupSubtitle(backup: ContentBackupUi): String {
    val days = backup.daysSinceBackup
    return when {
        backup.isRunning -> stringResource(Res.string.contentBackupRunning)
        days == null -> stringResource(Res.string.contentBackupNever)
        else -> stringResource(Res.string.contentBackupDaysAgo, days)
    }
}
