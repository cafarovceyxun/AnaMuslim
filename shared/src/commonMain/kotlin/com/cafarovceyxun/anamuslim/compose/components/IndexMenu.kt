package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.*
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

private data class IndexMenuItemGroup(
    val items: List<IndexMenuItem>,
)

private data class IndexMenuItem(
    val icon: DrawableResource,
    val title: StringResource,
    val iconTint: Color? = null,
    val textColor: Color? = null,
    val onClick: () -> Unit,
)

@Composable
private fun getItems(): List<IndexMenuItemGroup> {
    val actions = LocalIndexMenuActions.current
    return listOf(
        IndexMenuItemGroup(
            listOfNotNull(
                IndexMenuItem(
                    Res.drawable.ic_bookmarks,
                    Res.string.strTitleBookmarks,
                    onClick = actions.onOpenBookmarks
                ),
                IndexMenuItem(
                    Res.drawable.dr_icon_settings,
                    Res.string.strTitleSettings,
                    onClick = actions.onOpenSettings
                ),
                IndexMenuItem(
                    Res.drawable.icon_clean,
                    Res.string.titleStorageCleanup,
                    onClick = actions.onOpenStorageCleanup
                ),
                // Dropped where there is no store listing to open (iOS, until the app ships).
                actions.onOpenPlayStore?.let { openStore ->
                    IndexMenuItem(
                        Res.drawable.dr_icon_update_app,
                        Res.string.strLabelUpdate,
                        onClick = openStore
                    )
                },
                IndexMenuItem(
                    Res.drawable.icon_import_export,
                    Res.string.titleExportData,
                    onClick = actions.onOpenExportImport
                ),
            )
        ),
        IndexMenuItemGroup(
            listOfNotNull(
                IndexMenuItem(
                    Res.drawable.dr_icon_info,
                    Res.string.strTitleAboutUs,
                    onClick = actions.onOpenAboutUs
                ),
                actions.onRateApp?.let { rate ->
                    IndexMenuItem(
                        Res.drawable.dr_icon_rate,
                        Res.string.strTitleRateApp,
                        onClick = rate
                    )
                },
                actions.onShareApp?.let { share ->
                    IndexMenuItem(
                        Res.drawable.dr_icon_share,
                        Res.string.strTitleShareApp,
                        onClick = share
                    )
                },
            )
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexMenuButton() {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)

    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthDp = with(density) { screenWidth.toDp() }
    
    val sheetMaxWidth = screenWidthDp * .95f
    val sheetWidthDiff = (screenWidthDp - sheetMaxWidth) / 2

    SimpleTooltip(text = stringResource(Res.string.strTitleMenu)) {
        IconButton(
            modifier = Modifier.size(40.dp),
            onClick = {
                showMenu = true
            }
        ) {
            Icon(
                painter = painterResource(
                    Res.drawable.dr_icon_hamburger
                ),
                contentDescription = stringResource(Res.string.strTitleMenu),
                tint = colorScheme.onSurface
            )
        }
    }


    if (!showMenu) return

    ModalBottomSheet(
        modifier = Modifier
            .padding(
                bottom = sheetWidthDiff
            ),
        onDismissRequest = {
            showMenu = false
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = colorScheme.onSurface,
        shape = shapes.large,
        scrimColor = Color.Black.alpha(0.5f),
        dragHandle = null,
        sheetMaxWidth = minOf(sheetMaxWidth, BottomSheetDefaults.SheetMaxWidth),
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
        }
    ) {
        IndexMenuContent {
            showMenu = false
        }
    }
}


@Composable
fun IndexMenuContent(
    onClose: () -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }

    val maxMenuHeight = screenHeightDp * 0.8f
    val scrollState = rememberScrollState()
    val items = getItems()

    Column(
        modifier = Modifier
            .heightIn(max = maxMenuHeight)
            .background(colorScheme.surface, RoundedCornerShape(20.dp))
            .border(
                1.dp,
                colorScheme.outlineVariant.alpha(0.5f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_hamburger),
                contentDescription = null,
                modifier = Modifier.size(25.dp)
            )

            Text(
                text = stringResource(Res.string.strTitleMenu),
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .weight(1f),
                style = typography.titleLarge,
            )

            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                onClick = onClose
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_close),
                    contentDescription = stringResource(Res.string.strDescClose),
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = colorScheme.outlineVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalFadingEdge(scrollState, color = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(top = 12.dp, bottom = 20.dp)
            ) {
                items.forEachIndexed { groupIndex, group ->
                    group.items.forEachIndexed { _, item ->
                        IndexMenuItemRow(item, onClose)
                    }

                    if (groupIndex < items.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexMenuItemRow(
    item: IndexMenuItem,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                item.onClick()
                onClose()
            })
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = item.iconTint ?: LocalContentColor.current
        )

        Spacer(modifier = Modifier.width(17.dp))

        Text(
            text = stringResource(item.title),
            color = item.textColor ?: colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
