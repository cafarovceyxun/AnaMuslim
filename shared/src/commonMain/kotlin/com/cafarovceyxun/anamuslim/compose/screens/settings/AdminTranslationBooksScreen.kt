package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.COLUMN_TEXT
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationCatalogBook
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.viewModels.AdminTranslationViewModel
import org.jetbrains.compose.resources.painterResource

/**
 * Tərcümə kataloqu (`quran_translation_books`) — hansı kitabın adi istifadəçilərə açıq olduğunu
 * burada idarə edirik.
 *
 * Açarı çevirmək `is_public` sütununu yazır; klientlər tərcümə siyahısını **hər açılışda** kataloqdan
 * qurduğuna görə kitab **yeni buraxılış olmadan** görünür. Bağlı kitab admin üçün siyahıda qalır ki,
 * hazır olmadan sınana bilsin.
 */
@Composable
fun AdminTranslationBooksScreen() {
    val vm = viewModel { AdminTranslationViewModel() }
    val books by vm.books.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val message by vm.message.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    LaunchedEffect(message) {
        message?.let {
            PlatformUtils.showLongToast(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            AppBar(
                title = "Tərcümələr",
                actions = {
                    IconButton(painter = painterResource(Res.drawable.dr_icon_refresh)) { vm.load() }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = mainBottomNavigationOuterHeight() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Açıq kitab tərcümə siyahısında hamıya görünür. Bağlı kitabı yalnız giriş " +
                        "etmiş admin görür — hazırlanarkən sınamaq üçün.",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            items(books, key = { it.slug }) { book ->
                BookRow(
                    book = book,
                    enabled = !isBusy,
                    onToggle = { vm.setPublic(book.slug, it) },
                )
            }

            if (books.isEmpty()) {
                item {
                    Text(
                        text = if (isBusy) "Yüklənir..." else "Kataloq boşdur.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookRow(
    book: TranslationCatalogBook,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.book_name.ifBlank { book.slug },
                style = typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = book.author_name.ifBlank { "—" },
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
            Text(
                // Sütun adı admin üçün vacibdir: mətnin harada saxlandığını göstərir.
                text = "${book.slug} · ${book.source_column}" +
                    if (book.source_column == COLUMN_TEXT) " (əsas)" else "",
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = book.is_public,
            onCheckedChange = onToggle,
            enabled = enabled,
        )
    }
}
