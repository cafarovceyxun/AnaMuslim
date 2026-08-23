package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.formatDateTime
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarks
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.compose.theme.LocalAppTextScale

private const val PREVIEW_LIMIT = 10

/**
 * Ana səhifədə "Yadda saxlananlar" zolağı — oxuma keçmişi ilə eyni formatda. Ayə kartı birbaşa
 * oxuma ekranını, hədis kartı isə hədisin olduğu bölməni açır; "hamısı" siyahı ekranına aparır.
 */
@Composable
fun HomeSectionBookmarks() {
    val actions = LocalHomeActions.current
    val userRepo = remember { RepositoryProvider.userRepository }
    val quranRepo = remember { RepositoryProvider.quranRepository }

    val bookmarks by userRepo.getBookmarksFlow().collectAsState(emptyList())
    val hadithBookmarks by userRepo.getHadithBookmarksFlow().collectAsState(emptyList())

    val chapterNames by produceState(initialValue = emptyMap<Int, String>(), key1 = bookmarks) {
        if (bookmarks.isNotEmpty()) {
            value = quranRepo.getChapterNames(bookmarks.map { it.chapterNo })
        }
    }

    if (bookmarks.isEmpty() && hadithBookmarks.isEmpty()) return

    HomeSectionContainer {
        HomeSectionHeader(
            icon = Res.drawable.ic_bookmark_added,
            title = Res.string.strTitleBookmarks,
            horizontalPadding = SECTION_CONTENT_PADDING,
            onViewAllClick = actions.onOpenBookmarks,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = SECTION_CONTENT_PADDING),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                bookmarks.take(PREVIEW_LIMIT),
                key = { "verse-${it.id}" },
            ) { bookmark ->
                VerseBookmarkCard(
                    bookmark = bookmark,
                    chapterName = chapterNames[bookmark.chapterNo] ?: "${bookmark.chapterNo}",
                    onOpen = {
                        ReaderUiHooks.openVerseRange?.invoke(
                            bookmark.chapterNo,
                            bookmark.fromVerseNo,
                            bookmark.toVerseNo,
                        )
                    },
                )
            }

            items(
                hadithBookmarks.take(PREVIEW_LIMIT),
                key = { "hadith-${it.hadithId}" },
            ) { bookmark ->
                HadithBookmarkCard(
                    bookmark = bookmark,
                    onOpen = {
                        val volumeSlug = bookmark.volumeSlug
                        if (volumeSlug != null) {
                            actions.onOpenHadithItem(
                                volumeSlug,
                                bookmark.bookSlug,
                                bookmark.chapterSlug,
                                bookmark.subChapterSlug,
                                bookmark.title,
                            )
                        } else {
                            // Köhnə yazılarda bölmə məlumatı yoxdur — siyahı ekranına yönləndiririk.
                            actions.onOpenBookmarks()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun VerseBookmarkCard(
    bookmark: BookmarkEntity,
    chapterName: String,
    onOpen: () -> Unit,
) {
    BookmarkCardShell(onOpen = onOpen) {
        val verses = if (bookmark.fromVerseNo == bookmark.toVerseNo) {
            "${bookmark.fromVerseNo}"
        } else {
            "${bookmark.fromVerseNo}-${bookmark.toVerseNo}"
        }

        Text(
            text = "$chapterName ${bookmark.chapterNo}:$verses",
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurface,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = formatDateTime(bookmark.dateTime, "d MMM, HH:mm"),
            style = typography.labelSmall.copy(fontSize = 10.sp * LocalAppTextScale.current),
            color = colorScheme.onSurface.alpha(0.4f),
            maxLines = 1,
        )
    }
}

@Composable
private fun HadithBookmarkCard(
    bookmark: HadithBookmarkEntity,
    onOpen: () -> Unit,
) {
    BookmarkCardShell(onOpen = onOpen) {
        Text(
            text = "${bookmark.title} ${bookmark.hadithNo}",
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurface,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = formatDateTime(bookmark.dateTime, "d MMM, HH:mm"),
            style = typography.labelSmall.copy(fontSize = 10.sp * LocalAppTextScale.current),
            color = colorScheme.onSurface.alpha(0.4f),
            maxLines = 1,
        )
    }
}

@Composable
private fun BookmarkCardShell(
    onOpen: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .clip(shapes.medium)
            .border(0.8.dp, colorScheme.outlineVariant.alpha(0.25f), shapes.medium)
            .clickable(onClick = onOpen),
        color = colorScheme.surfaceVariant.alpha(0.2f),
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}
