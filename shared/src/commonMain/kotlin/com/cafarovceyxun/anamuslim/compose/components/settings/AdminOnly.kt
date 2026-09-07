package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel

/**
 * İdarəetmə ekranlarının görünüş qapısı.
 *
 * Əvvəl bu qapı **yalnız** `SettingsMainScreen`-dəki `if (isAdmin)` idi; idarəetmə bölməsi
 * Ayarlardan çıxarılıb qısayola bağlandığına görə (bax [com.cafarovceyxun.anamuslim.compose.screens.settings.AdminHubScreen])
 * yoxlama ekranların özünə köçürüldü. Səbəb: route-lar naviqasiya qrafında qalır və qısayol köhnə
 * ola bilər — girişsiz istifadəçi route-a düşəndə admin UI-ni görməməlidir.
 *
 * ⚠️ Bu, təhlükəsizlik sərhədi **deyil** — yazma icazələri Supabase RLS-indədir.
 */
@Composable
fun AdminOnly(title: String, content: @Composable () -> Unit) {
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsState()

    if (session != null) {
        content()
        return
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = { AppBar(title = title) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Bu bölmə üçün giriş tələb olunur.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
    }
}
