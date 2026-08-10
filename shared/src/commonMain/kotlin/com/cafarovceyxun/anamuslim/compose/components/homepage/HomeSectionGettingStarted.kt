package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.homeGettingStartedMessage
import com.cafarovceyxun.anamuslim.resources.homeGettingStartedTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shown on the homepage while the user has nothing saved yet.
 *
 * Every other homepage section returns early when its own list is empty, so a brand-new install
 * used to land on a page that was completely blank below the app bar — which reads as a broken
 * screen rather than an empty one. This fills that gap and disappears on its own as soon as any
 * history or bookmark exists.
 *
 * Deliberately has no buttons: the Quran and Hadith tabs are already one tap away in the bottom
 * bar, and adding a navigation callback here would mean another defaulted lambda on a shared
 * screen — the pattern that has repeatedly shipped as a button that silently does nothing on iOS.
 */
@Composable
fun HomeSectionGettingStarted() {
    val userRepo = remember { RepositoryProvider.userRepository }

    val histories by userRepo.getHistoriesFlow(1).collectAsState(emptyList())
    val hadithHistories by userRepo.getHadithHistoriesFlow(1).collectAsState(emptyList())
    val bookmarks by userRepo.getBookmarksFlow().collectAsState(emptyList())
    val hadithBookmarks by userRepo.getHadithBookmarksFlow().collectAsState(emptyList())

    val hasAnything = histories.isNotEmpty() ||
        hadithHistories.isNotEmpty() ||
        bookmarks.isNotEmpty() ||
        hadithBookmarks.isNotEmpty()

    if (hasAnything) return

    HomeSectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SECTION_CONTENT_PADDING, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_read_quran),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colorScheme.primary,
            )

            Text(
                text = stringResource(Res.string.homeGettingStartedTitle),
                style = typography.titleSmall,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(Res.string.homeGettingStartedMessage),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
