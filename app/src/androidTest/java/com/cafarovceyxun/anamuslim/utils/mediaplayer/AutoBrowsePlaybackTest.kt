package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives [RecitationService] the way a car does.
 *
 * Deliberately uses the **framework** [MediaBrowser] / [MediaController] instead of a media3
 * `MediaBrowser`: Android Auto is a legacy browser client, so its taps arrive as
 * `playFromMediaId` through media3's `MediaSessionLegacyStub` — a different route into the session
 * than a media3 controller takes. That route is what silently dropped every browse item when the
 * ids the tree handed out did not match the ids the play handler parsed, which is what got the app
 * rejected from Play in August 2026. Nothing about that failure was visible to the compiler, to the
 * unit tests, or to the app's own UI.
 *
 * Needs a connected device with network access: selecting a chapter resolves real audio.
 */
@RunWith(AndroidJUnit4::class)
class AutoBrowsePlaybackTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var browser: MediaBrowser
    private var controller: MediaController? = null

    @Before
    fun connect() {
        val connected = CountDownLatch(1)

        runOnMain {
            browser = MediaBrowser(
                context,
                ComponentName(context, RecitationService::class.java),
                object : MediaBrowser.ConnectionCallback() {
                    override fun onConnected() = connected.countDown()
                },
                null,
            )
            browser.connect()
        }

        assertTrue(
            "Could not connect to RecitationService as a browser",
            connected.await(20, TimeUnit.SECONDS),
        )

        runOnMain { controller = MediaController(context, browser.sessionToken) }
    }

    @After
    fun disconnect() {
        runOnMain {
            controller?.transportControls?.stop()
            if (::browser.isInitialized && browser.isConnected) browser.disconnect()
        }
    }

    @Test
    fun rootOffersChaptersAndReciters() {
        val children = childrenOf(browser.root)

        assertEquals(
            listOf("surahs_root", "reciters_root"),
            children.map { it.mediaId },
        )
        assertTrue("Root entries must be browsable", children.all { it.isBrowsable })
    }

    @Test
    fun chapterFolderListsAllChaptersAsPlayableItems() {
        val children = childrenOf("surahs_root")

        assertEquals(114, children.size)
        assertTrue("Chapters must be playable", children.all { it.isPlayable })
        assertEquals("chapter_1", children.first().mediaId)
        assertEquals("chapter_114", children.last().mediaId)
    }

    /**
     * The rejection itself: pick an item out of the browse tree and play it by its id, exactly as a
     * head unit does. Before the fix this left the player untouched — no error, no log, no playback.
     */
    @Test
    fun selectingAChapterPlaysThatChapter() {
        val chapter = childrenOf("surahs_root")[6] // 7. Əl-Əraf
        val mediaId = chapter.mediaId

        assertEquals("chapter_7", mediaId)

        runOnMain { controller!!.transportControls.playFromMediaId(mediaId, null) }

        // The service marks the chapter as being resolved the moment the id is understood, before
        // any audio is fetched — so this half of the check holds even on a slow connection.
        assertTrue(
            "Selecting a browse item did not reach playback for chapter 7",
            waitFor(seconds = 15) {
                val state = RecitationService.sharedState.value
                state.resolvingChapterNo == 7 || state.currentVerse.chapterNo == 7
            },
        )

        assertTrue(
            "Chapter 7 was selected but playback never started",
            waitFor(seconds = 45) {
                val state = RecitationService.sharedState.value
                state.isPlaying && state.currentVerse.chapterNo == 7
            },
        )
    }

    private fun childrenOf(parentId: String): List<MediaBrowser.MediaItem> {
        val loaded = CountDownLatch(1)
        var children: List<MediaBrowser.MediaItem> = emptyList()

        runOnMain {
            browser.subscribe(
                parentId,
                object : MediaBrowser.SubscriptionCallback() {
                    override fun onChildrenLoaded(
                        parent: String,
                        items: MutableList<MediaBrowser.MediaItem>,
                    ) {
                        children = items.toList()
                        loaded.countDown()
                    }
                },
            )
        }

        assertTrue(
            "Timed out loading children of $parentId",
            loaded.await(30, TimeUnit.SECONDS),
        )

        return children
    }

    private fun runOnMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private fun waitFor(seconds: Int, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + seconds * 1000L

        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(250)
        }

        return condition()
    }
}
