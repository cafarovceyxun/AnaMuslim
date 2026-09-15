package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.epochMillisAtLocalTime
import com.cafarovceyxun.anamuslim.utils.hijriDate
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement

/**
 * Adminin «ayı gördük» elanını ([LunarAnnouncement]) platformanın Ümmül-Qüra təqvimi ilə birləşdirir.
 *
 * **Niyə gün fərqi, ay adı yox:** `hijriDate` `expect/actual`-dır və hər iki platformada sistemin öz
 * təqvimindən gəlir. Onu əvəz etmək iki ayrı hicri implementasiyası yazmaq demək olardı. Əvəzinə
 * elanı **çevirmənin girişinə** sürüşdürmə kimi tətbiq edirik (`hijriDate(millis + gün)`) — mövcud
 * `KEY_LUNAR_OFFSET` ilə **eyni mexanizm**, ona görə vidcet, paylaşma şəkli və ekran heç nə bilmədən
 * düzgün nəticə verir.
 *
 * İki ayrı şey verir və qarışdırılmamalıdır:
 * - [offsetDaysFor] — **tarixi** yerinə oturdan gün fərqi (bütün aylara tətbiq olunur).
 * - [overrideAt] — **ayın uzunluğunu** (29/30) adminin dediyi kimi kəsən üst-yazma. Sürüşdürmə
 *   ayın uzunluğunu dəyişmir: platforma 29 deyirsə, sürüşdürülmüş ay da 29 qalır. Admin 30 deyibsə
 *   fərqi yalnız bu üst-yazma bağlayır.
 */
object LunarCalendar {

    /**
     * Elanı platformanın təqvimi ilə üst-üstə salan gün fərqi, tapılmasa null.
     *
     * Platformanın həmin qəməri ayın **1-i** saydığı miladi gün axtarılır; fərq elə oradan çıxır.
     * «Bu gün platforma nə deyir» üzərindən hesablamaq olmazdı: ay sərhədini keçəndə fərq xətti
     * qalmır.
     *
     * Axtarış [SEARCH_WINDOW_DAYS] günlə məhdudur və **mərkəzdən kənara** gedir (0, +1, −1, +2, …):
     * gözlə görmə ilə hesablama arasındakı real fərq bir-iki gündür, daha böyük fərq isə yanlış
     * yazılmış elan deməkdir — onu tətbiq etmək təqvimi tamam sürüşdürərdi.
     */
    fun offsetDaysFor(announcement: LunarAnnouncement): Int? {
        val announcedStart = IsoDate.toEpochDay(announcement.start_date) ?: return null

        for (delta in searchOrder) {
            val at = hijriAtEpochDay(announcedStart + delta) ?: continue
            val (day, month, year) = at

            if (day == 1 && month == announcement.hijri_month && year == announcement.hijri_year) {
                return delta
            }
        }

        return null
    }

    /**
     * [anchorMillis] anının düşdüyü elan — cari ayı tapmaq üçün. Siyahı istənilən sırada ola bilər.
     *
     * Aralıq elanın **öz** uzunluğu ilə ölçülür (platformanınkı ilə yox), ona görə admin 30 deyəndə
     * 30-cu gün də bu aya aiddir.
     */
    fun announcementAt(
        anchorMillis: Long,
        announcements: List<LunarAnnouncement>,
    ): LunarAnnouncement? {
        val day = localEpochDay(anchorMillis) ?: return null

        return announcements.firstOrNull { announcement ->
            val first = IsoDate.toEpochDay(announcement.start_date) ?: return@firstOrNull false
            day >= first && day < first + announcement.length_days
        }
    }

    /**
     * [anchorMillis] ayının uzunluq üst-yazması — [LunarMonth.spanContaining]-ə verilir.
     *
     * Elan yoxdursa null: platformanın öz ay uzunluğu qalır.
     */
    fun overrideAt(
        anchorMillis: Long,
        announcements: List<LunarAnnouncement>,
    ): LunarMonth.Override? = announcementAt(anchorMillis, announcements)?.toOverride()

    /** Elanın [LunarMonth]-un anladığı formaya çevrilmiş hali. */
    fun LunarAnnouncement.toOverride(): LunarMonth.Override? {
        val first = IsoDate.toEpochDay(start_date) ?: return null

        return LunarMonth.Override(
            month = hijri_month,
            year = hijri_year,
            firstEpochDay = first,
            lengthDays = length_days,
        )
    }

    /**
     * Elanların **cari** olanı: bu gün hansı elan olunmuş ayın içindəyiksə o, yoxsa ən son başlamışı.
     *
     * İkinci qol vacibdir: admin ayı elan etməyi bir-iki gün gecikdirsə də ötən ayın sürüşdürməsi
     * qüvvədə qalmalıdır, yoxsa təqvim elan gününə qədər platformanın öz tarixinə qayıdardı.
     */
    fun currentAnnouncement(
        nowMillis: Long,
        announcements: List<LunarAnnouncement>,
    ): LunarAnnouncement? {
        announcementAt(nowMillis, announcements)?.let { return it }

        val today = localEpochDay(nowMillis) ?: return null

        return announcements
            .filter { (IsoDate.toEpochDay(it.start_date) ?: Long.MAX_VALUE) <= today }
            .maxByOrNull { IsoDate.toEpochDay(it.start_date) ?: Long.MIN_VALUE }
    }

    // ── daxili ───────────────────────────────────────────────────────────────────────────────

    /** ±[SEARCH_WINDOW_DAYS], mərkəzdən kənara: 0, 1, −1, 2, −2, … */
    private val searchOrder: List<Int> = buildList {
        add(0)
        for (step in 1..SEARCH_WINDOW_DAYS) {
            add(step)
            add(-step)
        }
    }

    private const val SEARCH_WINDOW_DAYS = 5

    private fun hijriAtEpochDay(epochDay: Long): Triple<Int, Int, Int>? =
        noonMillisOf(epochDay)?.let(::hijriDate)

    /**
     * Günün **yerli günorta** anı — [LunarMonth] ilə eyni qayda. Gecəyarısı götürsəydik yay vaxtı
     * keçidində gün ya təkrarlanar, ya da atlanardı.
     */
    internal fun noonMillisOf(epochDay: Long): Long? =
        epochMillisAtLocalTime(IsoDate.fromEpochDay(epochDay), hour = 12, minute = 0)

    private fun localEpochDay(epochMillis: Long): Long? =
        IsoDate.toEpochDay(PrayerDay.localDateOfDevice(epochMillis))
}
