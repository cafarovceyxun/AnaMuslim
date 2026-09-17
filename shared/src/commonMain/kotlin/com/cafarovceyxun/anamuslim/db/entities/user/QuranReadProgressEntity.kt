package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cafarovceyxun.anamuslim.utils.reader.ReadType

/**
 * Oxunub qurtarılmış **surə / cüz / hizb** — naviqator siyahılarındakı ✓ nişanı bunu oxuyur.
 *
 * Düyün oxucunun açdığı vahiddir, ayə deyil: `ReaderViewType` elə bu üç formadan biridir, ona görə
 * «bitdi» siqnalı da həmin vahidə yazılır (hədis tərəfindəki yarpaq qaydasının eynisi —
 * [HadithReadProgressEntity]).
 *
 * ⚠️ **Cüz/hizb tamamlanması surələrdən çıxarılmır.** Cüz surə sərhədini kəsir (1-ci cüz Fatihə +
 * Bəqərə 1–141-dir), ona görə «bu cüzün bütün surələri bitib» ≠ «cüz bitib». Hər vahid öz sətrini
 * alır; xətm sayğacı isə yalnız `chapter` sətirlərinə baxır.
 *
 * Tarixçədən ([ReadHistoryEntity]) fərqi hədisdəki ilə eynidir: ora **harada qaldın**, bura **nəyi
 * bitirdin**. Biri son mövqedir və üzərinə yazılır, digəri yığılır.
 */
@Entity(tableName = "quran_read_progress")
data class QuranReadProgressEntity(
    /** `"<readType>:<nodeNo>"` — bax [keyOf]. */
    @PrimaryKey @ColumnInfo(name = "node_key") val nodeKey: String,
    @ColumnInfo(name = "read_type") val readType: String,
    @ColumnInfo(name = "node_no") val nodeNo: Int,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
) {
    companion object {
        /**
         * Düyün açarı. Tip açarın **içindədir**, çünki surə 2 ilə cüz 2 ayrı şeylərdir və ikisi də
         * eyni cədvəldə yaşayır.
         */
        fun keyOf(readType: ReadType, nodeNo: Int): String = "${readType.value}:$nodeNo"
    }
}
