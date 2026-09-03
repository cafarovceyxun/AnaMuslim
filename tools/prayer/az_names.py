# -*- coding: utf-8 -*-
# GeoNames `asciiname` → Azərbaycan dilində rəsmi ad. Ana bazar olduğu üçün əl ilə yoxlanılıb.
# Qeyd: Divichibazar 2010-da Şabran, Pushkino isə Biləsuvar adlandırılıb.
AZ_NAMES = {
    "Baku": "Bakı", "Sumgayit": "Sumqayıt", "Ganja": "Gəncə", "Khirdalan": "Xırdalan",
    "Yevlakh": "Yevlax", "Mingachevir": "Mingəçevir", "Naxcivan": "Naxçıvan",
    "Lankaran": "Lənkəran", "Qaracuxur": "Qaraçuxur", "Lerik": "Lerik", "Sirvan": "Şirvan",
    "Sheki": "Şəki", "Xacmaz": "Xaçmaz", "Bakixanov": "Bakıxanov", "Goeycay": "Göyçay",
    "Bilajari": "Biləcəri", "Barda": "Bərdə", "Shamkhir": "Şəmkir", "Aghjabadi": "Ağcabədi",
    "Shamakhi": "Şamaxı", "Mastaga": "Maştağa", "Saray": "Saray", "Aghdam": "Ağdam",
    "Salyan": "Salyan", "Hovsan": "Hövsan", "Jalilabad": "Cəlilabad", "Imishli": "İmişli",
    "Zaqatala": "Zaqatala", "Lokbatan": "Lökbatan", "Sabirabad": "Sabirabad",
    "Amirdzhan": "Əmircan", "Ismayilli": "İsmayıllı", "Siyazan": "Siyəzən", "Fuzuli": "Füzuli",
    "Buzovna": "Buzovna", "Agdas": "Ağdaş", "Divichibazar": "Şabran", "Haciqabul": "Hacıqabul",
    "Quba": "Quba", "Aghsu": "Ağsu", "Zabrat": "Zabrat", "Sabuncu": "Sabunçu", "Gazakh": "Qazax",
    "Mehdiabad": "Mehdiabad", "Tartar": "Tərtər", "Saatli": "Saatlı", "Goygol": "Göygöl",
    "Shusha": "Şuşa", "Kyurdarmir": "Kürdəmir", "Neftcala": "Neftçala", "Jorat": "Corat",
    "Pushkino": "Biləsuvar", "Gobu": "Qobu", "Hokmali": "Hökməli", "Ahmadbayli": "Əhmədbəyli",
    "Nehram": "Nehrəm", "Qusar": "Qusar", "Ujar": "Ucar", "Govlar": "Qovlar",
    "Beylagan": "Beyləqan", "Mardakan": "Mərdəkan", "Astara": "Astara",
    "Yeni Suraxani": "Yeni Suraxanı", "Haji Zeynalabdin": "Hacı Zeynalabdin", "Biny Selo": "Binə",

    # ── cities5000 ilə gələn 69 məntəqə (2026-09-03) ────────────────────────────────────
    # ⚠️ Bu blok MAŞIN TƏKLİFİDİR, yuxarıdakı 65 kimi əl ilə təsdiqlənməyib.
    # GeoNames-in `az` sahəsi burada daha da pisdir — sovet adları («Prişib» = Göytəpə,
    # «Qasım Ismayılov» = Goranboy, «Bir May» = Bəhramtəpə) və açıq səhvlər («İçəri» = Zəngilan,
    # «Kiadabak» = Gədəbəy). Ona görə heç biri avtomatik götürülmədi.
    # Şübhəli qalanlar: Qarayeri (GeoNames «Cırdaxan» yazır — başqa kənddir),
    # Dəli Məmmədli, Bankə (mənbədə «Severo-Vostotchnyi Bank»), Xocasan (GeoNames «Xocəsən»).
    "Belokany": "Balakən", "Gadabay": "Gədəbəy", "Binagadi": "Binəqədi", "Julfa": "Culfa",
    "Xudat": "Xudat", "Pirallahi": "Pirallahı", "Qobustan": "Qobustan", "Geytepe": "Göytəpə",
    "Tovuz": "Tovuz", "Aghstafa": "Ağstafa", "Qax": "Qax", "Qabala": "Qəbələ",
    "Badamdar": "Badamdar", "Samukh": "Samux", "Balakhani": "Balaxanı", "Ordubad": "Ordubad",
    "Jahri": "Cəhri", "Zardob": "Zərdab", "Dashkasan": "Daşkəsən", "Gobustan": "Qobustan",
    "Zyrya": "Zirə", "Hindarkh": "Hindarx", "Digah": "Digah", "Kur": "Kür", "Turkan": "Türkan",
    "Masally": "Masallı", "Jabrayil": "Cəbrayıl", "Jeyranbatan": "Ceyranbatan",
    "Goranboy": "Goranboy", "Aliabad": "Əliabad", "Bilajer": "Bilgəh", "Ramana": "Ramana",
    "Naftalan": "Naftalan", "Yayci": "Yaycı", "Kalbajar": "Kəlbəcər", "Zayam": "Zəyəm",
    "Ashaghi Guzdak": "Aşağı Güzdək", "Horadiz": "Horadiz", "Nardaran": "Nardaran",
    "Qalaqayin": "Qalaqayın", "Yardimli": "Yardımlı", "Sharur City": "Şərur",
    "Gizilhajili": "Qızılhacılı", "Zangilan": "Zəngilan", "Bahramtepe": "Bəhramtəpə",
    "Chinarli": "Çinarlı", "28 May": "28 May", "Severo-Vostotchnyi Bank": "Bankə",
    "Qubadli": "Qubadlı", "Aran": "Aran", "Oguz": "Oğuz", "Kyurdakhany": "Kürdəxanı",
    "Cinarli": "Çinarlı", "Givrag": "Qıvraq", "Mincivan": "Mincivan", "Garayeri": "Qarayeri",
    "Babak": "Babək", "Dunyamalilar": "Dünyamalılar", "Boradigah": "Boradigah",
    "Khojaly": "Xocalı", "Avsar": "Avşar", "Dallar": "Dəllər", "Dalimammadli": "Dəli Məmmədli",
    "Bum": "Bum", "Khojasan": "Xocasan", "Dollyar-Dzhagir": "Dəllər Cəyir",
    "Khojavend": "Xocavənd", "Lacin": "Laçın", "Khizi": "Xızı",
}
