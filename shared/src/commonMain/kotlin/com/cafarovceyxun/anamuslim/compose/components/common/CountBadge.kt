package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bildiriş üslubunda qırmızı say nişanı — sətrin sağında, oxun yanında.
 *
 * [count] sıfır və ya mənfidirsə **heç nə çəkmir**: çağıran tərəf ayrıca `if` yazmasın deyə boşluq
 * da qalmır.
 */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(colorScheme.error, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // Üç rəqəm sətri dartıb qonşu mətni sıxışdırır — 99-dan sonrası kəsilir.
            text = if (count > 99) "99+" else count.toString(),
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onError,
            maxLines = 1,
        )
    }
}
