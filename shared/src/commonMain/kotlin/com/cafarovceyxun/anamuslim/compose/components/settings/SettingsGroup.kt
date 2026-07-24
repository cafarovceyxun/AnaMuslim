package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * A single rounded card that holds a set of related settings rows, stacked with hairline dividers
 * between them — the grouped-list look. Rows added through [SettingsGroupScope.item] should be the
 * "flat" variants (e.g. `SettingsItem(flat = true)` or `SwitchItem`), since this card already draws
 * the shared surface and elevation. An optional [title] renders a category label above the card.
 *
 * Empty groups (every item filtered out) render nothing, so callers can add rows conditionally
 * without leaving a stray header or an empty card behind.
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: SettingsGroupScope.() -> Unit,
) {
    val scope = SettingsGroupScope().apply(content)
    if (scope.items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            ListItemCategoryLabel(title = title)
        }

        val shape = MaterialTheme.shapes.large
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, shape)
                .clip(shape)
                .background(colorScheme.surface),
        ) {
            scope.items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
                item()
            }
        }
    }
}

class SettingsGroupScope {
    internal val items = mutableListOf<@Composable () -> Unit>()

    /** Add a row to the group. Skip the call to omit a row (e.g. a platform-gated preference). */
    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}
