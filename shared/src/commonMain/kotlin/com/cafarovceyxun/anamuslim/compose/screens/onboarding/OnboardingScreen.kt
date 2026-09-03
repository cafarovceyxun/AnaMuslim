package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_language
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.ic_bell
import com.cafarovceyxun.anamuslim.resources.onboardDescLanguage
import com.cafarovceyxun.anamuslim.resources.onboardDescTheme
import com.cafarovceyxun.anamuslim.resources.onboardDescNotifications
import com.cafarovceyxun.anamuslim.resources.onboardDescResources
import com.cafarovceyxun.anamuslim.resources.strLabelBack
import com.cafarovceyxun.anamuslim.resources.strLabelNext
import com.cafarovceyxun.anamuslim.resources.onboardTitleNotifications
import com.cafarovceyxun.anamuslim.resources.onboardTitleResources
import com.cafarovceyxun.anamuslim.resources.strLabelStart
import com.cafarovceyxun.anamuslim.resources.strTitleAppLanguage
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val onboardingIcons = listOf(
    Res.drawable.dr_icon_language,
    Res.drawable.dr_icon_theme,
    // Downloading is what the last page is for — the translations glyph undersold it once the page
    // grew to cover word-by-word, hadith, the reciter and the script.
    Res.drawable.dr_icon_download,
    Res.drawable.ic_bell,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    /**
     * Leaves onboarding without completing it — back-press on the first page. The host closes its
     * own screen (Android: `Activity.finish()`), which common code cannot do itself.
     */
    onExit: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    // Notification permission (null below Android 13, where none is needed).
    //
    // ⚠️ Burada `request()` ÇAĞIRILMIR. Əvvəllər ekran açılan kimi izahatsız sistem dialoqu atılır və
    // nəticə heç yerdə yoxlanmırdı: istifadəçi «İmtina» seçirdi, sistem bir daha soruşmurdu, tətbiq
    // isə bunu bilmədən namaz və günün ayəsi bildirişlərini «açıq» göstərməyə davam edirdi. İndi
    // icazə 4-cü səhifədə kontekstlə istənilir ([OnboardingNotificationsPage]).
    val notificationPermission = rememberNotificationPermission()

    val items = listOf(
        Res.string.strTitleAppLanguage to Res.string.onboardDescLanguage,
        Res.string.strTitleTheme to Res.string.onboardDescTheme,
        // Not "select translations" any more: the page also carries word-by-word, hadith, the
        // reciter and the mushaf script.
        Res.string.onboardTitleResources to Res.string.onboardDescResources,
        Res.string.onboardTitleNotifications to Res.string.onboardDescNotifications,
    )
    val pageCount = items.size

    var savedPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedPage,
        initialPageOffsetFraction = 0f,
        pageCount = { pageCount },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { savedPage = it }
    }

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onExit()
        }
    }

    val lastPage = pageCount - 1

    // Bildiriş qapısı. `?: true` **kritikdir**: Android 12 və aşağısında icazə anlayışı yoxdur
    // (`rememberNotificationPermission()` null qaytarır) və qapı orada avtomatik açıq olmalıdır —
    // əks halda həmin cihazlarda «Başla» heç vaxt aktivləşməzdi və tətbiq ilk açılışda kilidlənərdi.
    val notificationsSettled = notificationPermission?.isGranted ?: true

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {

            val page = pagerState.currentPage

            Column(
                modifier = Modifier
                    .background(colorScheme.surfaceContainer)
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // No skip button: onboarding is mandatory, so the only way past it is through the
                // pages. The status-bar inset it used to carry still has to be reserved.
                Spacer(modifier = Modifier.statusBarsPadding())

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(onboardingIcons[page]),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = page,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "onboardingTitle",
                ) { p ->
                    val item = items.get(p)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(item.first),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(item.second),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = colorScheme.outline.alpha(0.2f)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    verticalAlignment = Alignment.Top,
                ) { pageIndex ->
                    // Capping here covers all three pages at once; each page is a plain
                    // `fillMaxWidth` column, which on a tablet would otherwise strand every
                    // trailing radio button ~1000dp away from its own label.
                    ReadableWidthColumn {
                        when (pageIndex) {
                            0 -> OnboardingLanguagePage()
                            1 -> OnboardingThemePage()
                            2 -> OnboardingTranslationsPage()
                            3 -> OnboardingNotificationsPage(permission = notificationPermission)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = colorScheme.surfaceContainer,
                shadowElevation = 12.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (pagerState.currentPage > 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                                contentDescription = stringResource(Res.string.strLabelBack),
                                tint = colorScheme.onSurface,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(pageCount) { i ->
                            val selected = i == pagerState.currentPage
                            val dotWidth by animateDpAsState(
                                targetValue = if (selected) 22.dp else 7.dp,
                                animationSpec = tween(220),
                                label = "dotW",
                            )

                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(dotWidth)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colorScheme.primary),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colorScheme.outlineVariant.alpha(0.45f)),
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage == lastPage) {
                                onComplete()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        // Son səhifədə «Başla» yalnız bildiriş icazəsi həll olunandan sonra işləyir.
                        // Çıxış yolu var: icazə daimi rədd edilibsə səhifə «Ayarları aç» düyməsi
                        // göstərir və istifadəçi qayıdanda `ON_RESUME` vəziyyəti yeniləyir.
                        enabled = pagerState.currentPage != lastPage || notificationsSettled,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp,
                        ),
                    ) {
                        Text(
                            if (pagerState.currentPage == lastPage) {
                                stringResource(Res.string.strLabelStart)
                            } else {
                                stringResource(Res.string.strLabelNext)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
