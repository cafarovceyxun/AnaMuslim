package com.cafarovceyxun.anamuslim.utils.quran

object AzerbaijaniSurahNames {
    private val names = mapOf(
        1 to "Fatihə", 2 to "Bəqərə", 3 to "Ali-İmran", 4 to "Nisə", 5 to "Maidə",
        6 to "Ən’am", 7 to "Əraf", 8 to "Ənfal", 9 to "Bəraə", 10 to "Yunus",
        11 to "Hud", 12 to "Yusuf", 13 to "Ra’d", 14 to "İbrahim", 15 to "Hicr",
        16 to "Nəhl", 17 to "İsra", 18 to "Kəhf", 19 to "Məryəm", 20 to "Taha",
        21 to "Ənbiya", 22 to "Həcc", 23 to "Muminun", 24 to "Nur", 25 to "Furqan",
        26 to "Şuara", 27 to "Nəml", 28 to "Qasas", 29 to "Ankəbut", 30 to "Rum",
        31 to "Loğman", 32 to "Səcdə", 33 to "Əhzab", 34 to "Səbə", 35 to "Fatir",
        36 to "Yasin", 37 to "Saffət", 38 to "Sad", 39 to "Zumər", 40 to "Ğafir",
        41 to "Fussilət", 42 to "Şura", 43 to "Zuxruf", 44 to "Duxan", 45 to "Casiyə",
        46 to "Əhqaf", 47 to "Muhəmməd", 48 to "Fəth", 49 to "Hucurat", 50 to "Qaf",
        51 to "Zariyat", 52 to "Tur", 53 to "Nəcm", 54 to "Qəmər", 55 to "Ər-Rahmən",
        56 to "Vaqiə", 57 to "Hədid", 58 to "Mücadilə", 59 to "Həşr", 60 to "Mumtəhənə",
        61 to "Saff", 62 to "Cümə", 63 to "Munafiqun", 64 to "Təğabun", 65 to "Talaq",
        66 to "Təhrim", 67 to "Mülk", 68 to "Qələm", 69 to "Haqqa", 70 to "Məaric",
        71 to "Nuh", 72 to "Cin", 73 to "Muzzəmmil", 74 to "Muddəssir", 75 to "Qiyamət",
        76 to "İnsan", 77 to "Mursələt", 78 to "Nəbə", 79 to "Naziat", 80 to "Abəsə",
        81 to "Təkvir", 82 to "İnfitar", 83 to "Mutaffifin", 84 to "İnşiqaq", 85 to "Buruc",
        86 to "Tariq", 87 to "Ə’lə", 88 to "Ğaşiyə", 89 to "Fəcr", 90 to "Bələd",
        91 to "Şəms", 92 to "Leyl", 93 to "Duha", 94 to "İnşirah", 95 to "Tin",
        96 to "Aləq", 97 to "Qadr", 98 to "Beyyinə", 99 to "Zilzal", 100 to "Adiyat",
        101 to "Qariə", 102 to "Təkəsur", 103 to "Əsr", 104 to "Huməzə", 105 to "Fil",
        106 to "Qureyş", 107 to "Məun", 108 to "Kövsər", 109 to "Kafirun", 110 to "Nəsr",
        111 to "Məsəd", 112 to "İxlas", 113 to "Fələq", 114 to "Nəs"
    )

    private val meanings = mapOf(
        1 to "Açılış", 2 to "İnək", 3 to "İmran Ailəsi", 4 to "Qadınlar", 5 to "Ziyafət süfrəsi",
        6 to "Mal-qara, ev heyvanları", 7 to "", 8 to "Qənimətlər", 9 to "Tövbə", 10 to "",
        11 to "", 12 to "", 13 to "Göy gurultusu", 14 to "", 15 to "",
        16 to "Arı", 17 to "Gecə yolculuğu", 18 to "Mağara", 19 to "", 20 to "",
        21 to "Nəbilər", 22 to "", 23 to "İman edənlər/Möminlər", 24 to "", 25 to "Haqla batili ayıran, fərqləndirən",
        26 to "Şairlər", 27 to "Dişi qarışqa", 28 to "Hadisələri ardıcıl olan rəvayətlər", 29 to "Dişi Hörümçək", 30 to "",
        31 to "", 32 to "", 33 to "Hizblər, Qruplar", 34 to "", 35 to "Yoxdan Var Edən, Yaradan",
        36 to "", 37 to "Səf-səf düzülənlər", 38 to "", 39 to "bir araya gələn topluluq", 40 to "Bağışlayan",
        41 to "Ayrıntılı, Açıqlanmış, Detaylı", 42 to "Məsləhətləşmə, müşavirə", 43 to "Bəzək əşyaları, göstəriş", 44 to "Duman, Tüstü", 45 to "Diz çökmüş",
        46 to "Qum təpələri", 47 to "", 48 to "", 49 to "Hücrələr, otaqlar", 50 to "",
        51 to "Saçıb-sovuranlar", 52 to "Tur Dağı", 53 to "Ulduz", 54 to "Ay", 55 to "Mərhəmətli",
        56 to "Baş verəcək hadisə", 57 to "Dəmir", 58 to "Müdafiə/Mübahisə edən qadın", 59 to "Toplanma", 60 to "İmtahan edilən, sorğulanan qadın",
        61 to "Səf, nizamlı sıra", 62 to "Toplanma", 63 to "Münafiqlər", 64 to "Aldanış", 65 to "Boşanma",
        66 to "Haram/Qadağan etmə", 67 to "Mülk, Hökmranlıq", 68 to "", 69 to "Haqq olan, gərçəkləşməsi mütləq olan", 70 to "Yüksəliş yolları/məqamı",
        71 to "", 72 to "", 73 to "Libasına bürünən", 74 to "Örtüyə bürünən", 75 to "Diriliş",
        76 to "", 77 to "Göndərilənlər", 78 to "Xəbər", 79 to "Çəkib çıxaranlar", 80 to "Qaş-qabağını sallama",
        81 to "Bükülmə, çevrilmə", 82 to "Yarılma, parçalanma", 83 to "Ölçüdə Aldadanlar, Hiylə edənlər", 84 to "Yarılma", 85 to "Bürclər",
        86 to "Gecə yolcusu, Sabaha doğru görünən ulduz", 87 to "Uca", 88 to "Bürüyən, Əhatə edən", 89 to "Sabah, Dan Vaxtı", 90 to "Şəhər",
        91 to "Günəş", 92 to "Gecə", 93 to "Quşluq vaxtı, Günəşin doğub parladığı zaman", 94 to "Fərahlıq", 95 to "Əncir",
        96 to "Asılıb tutulan/Laxtalanmış qan/Embrion", 97 to "", 98 to "Açıq-aydın dəlil", 99 to "zəlzələ", 100 to "Tövşüyərək sürətlə qaçanlar",
        101 to "Şiddətli səs, Toqquşma", 102 to "Çoxaltma yarışı", 103 to "Zaman", 104 to "Dediqoducu, Dili ilə çəkişdirən", 105 to "",
        106 to "", 107 to "Yardımlaşma", 108 to "Bol-bol xeyir", 109 to "Kafirlər", 110 to "Yardım, Dəstək",
        111 to "Burulmuş xurma lifi", 112 to "Xalis", 113 to "Yarılma, Çatlama, Qaranlığı Yaran", 114 to "İnsanlar"
    )

    fun getName(surahNo: Int): String? = names[surahNo]
    fun getMeaning(surahNo: Int): String? = meanings[surahNo]

    fun search(query: String): List<Int> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return names.filter { it.value.lowercase().contains(q) }.map { it.key }
    }
}
