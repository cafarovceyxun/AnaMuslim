package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.epochMillisAtLocalTime
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots

/**
 * Günün ayəsi/hədisi növbəsinin **vaxt cədvəli**: gündə beş sabit yuva.
 *
 * Yalnız hesabdır — nə şəbəkə, nə də saxlama bilir; ona görə hər iki platforma eyni cavabı alır və
 * testi asandır. Məzmunu [VotdNotificationContent], çatdırılmanı isə platforma qatı
 * (`VerseOfTheDayWorker` / `IosDailyReminder`) qurur.
 *
 * Saatlar qəsdən sabitdir: növbəni admin qurur, istifadəçi isə gündə beş bildirişin nə vaxt
 * gələcəyini əvvəlcədən bilir. Saatları ayara çıxarmaq lazım olsa yeganə dəyişməli yer [SLOT_TIMES]
 * -dır — qalan hər şey slot indeksi ilə işləyir.
 */
object DailyContentSchedule {

    data class SlotTime(val hour: Int, val minute: Int)

    /** Yerli saatla beş yuva. Sayı bazadakı `slot_index` CHECK-i ilə eynidir. */
    val SLOT_TIMES: List<SlotTime> = listOf(
        SlotTime(8, 0),
        SlotTime(12, 0),
        SlotTime(15, 0),
        SlotTime(18, 0),
        SlotTime(21, 0),
    )

    init {
        require(SLOT_TIMES.size == DailyContentSlots.COUNT)
    }

    /** `(tarix, slot)` yuvasının yerli anı, epoxa millisaniyəsi ilə; tarix pozulubsa null. */
    fun epochMillisOf(date: String, slot: Int): Long? {
        val time = SLOT_TIMES.getOrNull(slot) ?: return null
        return epochMillisAtLocalTime(date, time.hour, time.minute)
    }

    /** `HH:mm` — paneldə yuvanın etiketi. */
    fun label(slot: Int): String {
        val time = SLOT_TIMES.getOrNull(slot) ?: return ""
        return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }

    /**
     * Növbəyə yeni element hansı `(tarix, slot)` cütünə düşməlidir.
     *
     * [taken] — artıq tutulmuş `"tarix#slot"` açarları ([com.cafarovceyxun.anamuslim.utils.supabase.DailyContent.slotKey]).
     * Bu günün **vaxtı keçmiş** yuvaları boş olsa belə atlanır: ora qoyulan element heç vaxt
     * bildirilməzdi, admin isə onu növbədə görüb gözləyərdi.
     *
     * Boş yuva tapılmasa (nəzəri hal — [maxDaysAhead] gün doludur) null qaytarır.
     */
    fun firstFreeSlot(
        taken: Set<String>,
        today: String,
        nowMillis: Long,
        maxDaysAhead: Int = MAX_DAYS_AHEAD,
    ): Pair<String, Int>? {
        for (dayOffset in 0..maxDaysAhead) {
            val date = IsoDate.plusDays(today, dayOffset) ?: return null

            for (slot in 0 until DailyContentSlots.COUNT) {
                if ("$date#$slot" in taken) continue

                // Yalnız bugünkü yuvalar keçmiş ola bilər; sonrakı günlərdə yoxlama həmişə keçir.
                val at = epochMillisOf(date, slot)
                if (at != null && at <= nowMillis) continue

                return date to slot
            }
        }

        return null
    }

    /**
     * [count] elementi bugündən başlayaraq ardıcıl yuvalara paylayır — «10 ayə seçilibsə səhərə
     * davam etsin» tələbi buradadır: 5-dən sonrakı elementlər avtomatik növbəti günə keçir.
     *
     * Növbəni yenidən sıxlaşdırarkən ([com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository.compactQueue])
     * boşluqlar bu ardıcıllıqla doldurulur.
     */
    fun consecutiveSlots(
        count: Int,
        startDate: String,
        startSlot: Int = 0,
    ): List<Pair<String, Int>> {
        val result = ArrayList<Pair<String, Int>>(count)
        var date = startDate
        var slot = startSlot

        repeat(count) {
            result += date to slot
            slot++

            if (slot >= DailyContentSlots.COUNT) {
                slot = 0
                date = IsoDate.plusDays(date, 1) ?: date
            }
        }

        return result
    }

    /** Növbə nə qədər irəli planlaşdırılır — bildirişlərin əvvəlcədən qurulma üfüqü də budur. */
    const val MAX_DAYS_AHEAD = 60
}
