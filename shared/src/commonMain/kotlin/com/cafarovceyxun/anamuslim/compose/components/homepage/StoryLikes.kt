package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.storyLike
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionLocalStore
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource

/** `like_story(p_kind)`-in qəbul etdiyi növlər — həm də cihazdakı «bəyənilib» açarının prefiksi. */
enum class StoryKind(val key: String) {
    FEATURE("feature"),
    LUNAR("lunar"),
    ANNOUNCEMENT("announcement"),
}

/**
 * Hekayə bəyənmələri: ortaya iki dəfə vurmaq bəyənir, alt paneldəki ürək bəyənir/geri alır.
 *
 * Baxış sayğacı ilə eyni naxış — sayğac serverdə (`like_story()` RPC, `SECURITY DEFINER`),
 * «bu cihaz bəyənib» vəziyyəti cihazdadır ([SuggestionLocalStore]); kimlik saxlanmır. Ekrandakı say
 * **dərhal** dəyişir (optimistik), sonra serverin cavabı ilə düzəlir.
 *
 * Sorğu proses-ömürlü scope-dadır: hekayəni bəyənib dərhal bağlayanda pleyerin kompozisiya scope-u
 * ləğv olunur və sorğu yarıda qalardı (CLAUDE.md, `rememberCoroutineScope`).
 */
@Stable
internal class StoryLikes private constructor(private val kind: StoryKind) {
    private var liked by mutableStateOf<Set<Long>>(emptySet())
    private var counts by mutableStateOf<Map<Long, Int>>(emptyMap())

    suspend fun load() {
        liked = SuggestionLocalStore.likedStoryIds(kind.key)
    }

    fun isLiked(id: Long): Boolean = id in liked

    /** Serverdən gələn [fallback] üzərində bu sessiyada edilmiş dəyişiklik. */
    fun count(id: Long, fallback: Int): Int = counts[id] ?: fallback

    /** İki dəfə vurmaq — yalnız bəyənir; artıq bəyənilibsə heç nə etmir (animasiya yenə oynayır). */
    fun like(id: Long, currentCount: Int) {
        if (id !in liked) set(id, liked = true, currentCount = currentCount)
    }

    fun toggle(id: Long, currentCount: Int) = set(id, liked = id !in liked, currentCount = currentCount)

    private fun set(id: Long, liked: Boolean, currentCount: Int) {
        this.liked = if (liked) this.liked + id else this.liked - id
        val base = counts[id] ?: currentCount
        counts = counts + (id to (base + if (liked) 1 else -1).coerceAtLeast(0))

        scope.launch {
            SuggestionLocalStore.setStoryLiked(kind.key, id, liked)
            runCatching {
                SupabaseProvider.client.postgrest.rpc(
                    function = "like_story",
                    parameters = buildJsonObject {
                        put("p_kind", kind.key)
                        put("p_id", id)
                        put("p_delta", if (liked) 1 else -1)
                    },
                ).decodeAs<Int>()
            }.onSuccess { serverCount ->
                counts = counts + (id to serverCount)
            }.onFailure {
                AppLogger.d(TAG, "Like failed: ${it.message}")
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val TAG = "StoryLikes"

        // Növ başına **bir** obyekt, proses boyu: pleyer hər açılışda təzə obyekt qursaydı bu
        // sessiyada edilmiş sayı düzəlişi itərdi — hekayəni yenidən açanda ürək qırmızı, say isə
        // ekran siyahısındakı köhnə dəyər (0) görünürdü.
        private val instances = StoryKind.entries.associateWith { StoryLikes(it) }

        fun of(kind: StoryKind): StoryLikes = instances.getValue(kind)
    }
}

@Composable
internal fun rememberStoryLikes(kind: StoryKind): StoryLikes {
    val likes = remember(kind) { StoryLikes.of(kind) }
    LaunchedEffect(likes) { likes.load() }
    return likes
}

/**
 * İki dəfə vuranda kadrın ortasında açılıb sönən böyük ürək. [trigger] hər bəyənmədə artır —
 * eyni hekayəni ikinci dəfə vuranda da animasiya yenidən oynasın.
 */
@Composable
internal fun LikeBurst(trigger: Int, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(1f)
        scale.snapTo(0.4f)
        scale.animateTo(1.15f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        delay(250)
        alpha.animateTo(0f, tween(300))
    }

    Icon(
        imageVector = Icons.Rounded.Favorite,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier
            .size(110.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
    )
}

/** Alt paneldəki ürək + say: basmaq bəyənir və ya geri alır. */
@Composable
internal fun LikeButton(liked: Boolean, count: Int, textScale: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(Res.string.storyLike),
            tint = if (liked) LikedRed else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp * textScale,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val LikedRed = Color(0xFFFF3B5C)
