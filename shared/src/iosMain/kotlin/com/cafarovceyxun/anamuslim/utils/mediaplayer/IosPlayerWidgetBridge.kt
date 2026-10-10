package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.utils.IosPrayerWidgetBridge
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.shared.initSharedForIos
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue

/**
 * iOS qiraət pleyeri vidcetinin körpüsü — iki istiqamətdə.
 *
 * **Tətbiq → vidcet:** pleyerin ayəsi, oxuma və yüklənmə vəziyyəti dəyişəndə hazır
 * [PlayerWidgetSnapshot] App Group-a yazılır və vidcet yenidən çəkilir (namaz vidcetinin
 * [IosPrayerWidgetBridge] modeli; `WidgetCenter` Swift-only olduğu üçün yeniləmə host-un bağlaması
 * ilə gedir).
 *
 * **Vidcet → tətbiq:** vidcetin düymələri `AudioPlaybackIntent`-dir (`iosApp/WidgetIntents/`) və
 * iOS onları **tətbiqin öz prosesində** işlədir — lazım olsa tətbiqi arxa fonda qaldıraraq. Intent
 * [perform]-u çağırır. Tətbiq bağlı idisə UI heç vaxt qurulmur, ona görə paylaşılan qat burada
 * qaldırılır ([initSharedForIos] təkrar çağırışa davamlıdır).
 *
 * ⚠️ Pleyer yalnız lazım olanda yaradılır: onun konstruktoru audio sessiyasını aktivləşdirir. Snapshot
 * yazmaq üçün pleyer qurulmur — o hələ yoxdursa son dinlənən ayə götürülür ([attach]-ə bax).
 */
object IosPlayerWidgetBridge {

    const val SNAPSHOT_KEY = "player_widget_snapshot"

    /** `PlayerWidget.swift`-dəki `kind` ilə eyni. */
    const val WIDGET_KIND = "PlayerWidget"

    /** Vidcetə toxunanda açılan link: `anamuslim://player?chapter=2&verse=255`. */
    private const val URL_HOST = "player"

    private const val LOG_TAG = "player.widget"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json { encodeDefaults = true }

    /** Ardıcıl vəziyyət dəyişiklikləri yazıları üst-üstə salmasın — sonuncu qalib gəlsin. */
    private val writeMutex = Mutex()

    private var reloadHandler: ((String) -> Unit)? = null

    /** Yaradılıbsa pleyer; `null` — tətbiq bu prosesdə hələ heç nə oxumayıb. */
    private var player: RecitationPlayer? = null

    init {
        // Etiketlər və surə adları tətbiqin dilindədir — dil dəyişəndə yenidən yazılsın.
        // İlk emissiya atılır: ilk yazını bootstrap-ın [refresh]-i edir (DataStore o zamana qədər
        // qurulmayıb ola bilər).
        scope.launch {
            appLocaleFlow.map { it.languageTag }.distinctUntilChanged().drop(1).collect {
                runCatching { write() }
            }
        }
    }

    /** Swift host `WidgetCenter.shared.reloadTimelines(ofKind:)` verir. */
    fun setReloadHandler(handler: (kind: String) -> Unit) {
        reloadHandler = handler
    }

    /**
     * `IosBootstrap`-ın tənbəl pleyeri yarananda çağırır. Kilid ekranının [IosNowPlaying]-i ilə eyni
     * mənbəyə baxır: ayə, oxuma və yüklənmə.
     */
    internal fun attach(player: RecitationPlayer) {
        this.player = player

        scope.launch {
            combine(
                player.state.map { it.currentVerse }.distinctUntilChanged(),
                player.isPlayingState,
                player.isBufferingState,
                player.state.map { it.resolvingChapterNo != null }.distinctUntilChanged(),
            ) { verse, playing, buffering, resolving -> Triple(verse, playing, buffering || resolving) }
                .distinctUntilChanged()
                .collect { runCatching { write() } }
        }
    }

    /** Atəş-və-unut: bootstrap-dan çağırılır ki, vidcet ilk açılışdan son ayəni göstərsin. */
    fun refresh() {
        scope.launch { runCatching { write() } }
    }

    /**
     * Vidcet düyməsinin əmri. `action` — `iosApp/WidgetIntents/PlayerWidgetIntents.swift`-dəki
     * `PlayerWidgetAction` sətirləri ilə eynidir.
     *
     * Swift-dən **əsas axında** çağırılmalıdır (intent `@MainActor`-dur): Kotlin/Native suspend
     * funksiyanı başqa axından başlatmağa icazə vermir.
     */
    suspend fun perform(action: String) {
        initSharedForIos()

        // Pleyer burada yaranır (və [attach] olunur) — oxutma əmri üçün audio sessiyası lazımdır.
        val player = RecitationPlayerProvider.player
        val current = player.state.value.currentVerse

        withContext(Dispatchers.Main) {
            when (action) {
                ACTION_TOGGLE -> player.playPause(lastPlayedVerse())
                ACTION_NEXT_VERSE -> player.nextVerse()
                ACTION_PREVIOUS_VERSE -> player.previousVerse()
                ACTION_NEXT_CHAPTER -> jumpChapter(player, current, +1)
                ACTION_PREVIOUS_CHAPTER -> jumpChapter(player, current, -1)
                else -> AppLogger.d("$LOG_TAG: naməlum əmr '$action'")
            }
        }

        // Vəziyyət axını da yazacaq, amma intent bitəndə vidcet dərhal yenilənməlidir — iOS
        // düyməni basılmış saxlayır və snapshot gəlməyincə köhnə vəziyyəti göstərir.
        runCatching { write() }
    }

    /**
     * Vidcetə toxunuşun linkini açır. Bizim deyilsə `false` — host onu digər yönləndiricilərə verir.
     * Soyuq açılışda oxucu qarmağı hələ qeydiyyatda olmaya bilər, ona görə qısa müddət gözlənilir
     * (`IosVotdShortcut` ilə eyni).
     */
    fun openUrl(url: String): Boolean {
        val components = NSURLComponents(string = url)
        if (components.scheme != "anamuslim" || components.host != URL_HOST) return false

        val query = components.queryItems.orEmpty()
            .filterIsInstance<NSURLQueryItem>()
            .associate { it.name to it.value }
        val chapterNo = query["chapter"]?.toIntOrNull() ?: return true
        val verseNo = query["verse"]?.toIntOrNull() ?: 1

        scope.launch {
            repeat(50) {
                val open = ReaderUiHooks.openVerse
                if (open != null) {
                    withContext(Dispatchers.Main) { open(chapterNo, verseNo) }
                    return@launch
                }
                delay(100)
            }
            AppLogger.d("$LOG_TAG: oxucu qarmağı qeydiyyatdan keçmədi, toxunuş atıldı")
        }

        return true
    }

    private fun jumpChapter(player: RecitationPlayer, current: ChapterVersePair, delta: Int) {
        val target = (current.chapterNo + delta).coerceIn(QuranMeta.chapterRange)
        if (target != current.chapterNo) player.start(ChapterVersePair(target, 1))
    }

    private suspend fun lastPlayedVerse(): ChapterVersePair =
        RecitationPreferences.getLastPlayedVerse()?.takeIf { it.isValid } ?: ChapterVersePair(1, 1)

    private suspend fun write() = writeMutex.withLock {
        val player = player
        val snapshot = if (player != null) {
            PlayerWidgetSnapshotBuilder.build(
                verse = player.state.value.currentVerse,
                isPlaying = player.isPlaying,
                isLoading = player.isLoading,
            )
        } else {
            PlayerWidgetSnapshotBuilder.build(lastPlayedVerse(), isPlaying = false, isLoading = false)
        }

        val defaults = NSUserDefaults(suiteName = IosPrayerWidgetBridge.APP_GROUP_ID)
        defaults.setValue(json.encodeToString(snapshot), forKey = SNAPSHOT_KEY)
        defaults.synchronize()

        if (defaults.stringForKey(SNAPSHOT_KEY) == null) {
            AppLogger.d("$LOG_TAG: App Group yazıla bilmədi — entitlement yoxdur?")
            return@withLock
        }

        withContext(Dispatchers.Main) { reloadHandler?.invoke(WIDGET_KIND) }
    }

    const val ACTION_TOGGLE = "toggle"
    const val ACTION_NEXT_VERSE = "nextVerse"
    const val ACTION_PREVIOUS_VERSE = "previousVerse"
    const val ACTION_NEXT_CHAPTER = "nextChapter"
    const val ACTION_PREVIOUS_CHAPTER = "previousChapter"
}
