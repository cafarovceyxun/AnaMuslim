package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Redaktor üçün yerli videonun önizləməsi: idarəetmə düymələri yoxdur, [loopStartMillis]..
 * [loopEndMillis] aralığını dövrə vurur (kəsilmiş hissə necə görünəcəksə elə).
 *
 * [seek] hər yeni **instansiya** üçün bir dəfə tətbiq olunur (kimlik bərabərliyi) — eyni mövqeyə
 * ikinci dəfə sürüşdürmək də işləsin. Görünüş toxunuşu udmur: üstündəki kadr çərçivəsi Compose
 * jestlərini alır (iOS-da `interactionMode = null`, bax CLAUDE.md «UIKit interop toxunuşu udur»).
 *
 * Görünüş videonu öz qutusuna **sığdırır** (aspect fit) — çağıran qutunu videonun nisbətində
 * qurur ki, çərçivə payları kadrla üst-üstə düşsün.
 */
@Composable
expect fun VideoEditorPreview(
    url: String,
    modifier: Modifier,
    playing: Boolean,
    muted: Boolean,
    loopStartMillis: Long,
    loopEndMillis: Long,
    seek: SeekRequest?,
    onPosition: (Long) -> Unit,
)

/** Kimliklə müqayisə olunur — `data class` deyil. */
class SeekRequest(val millis: Long)
