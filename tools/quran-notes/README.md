# Haşiyə qeydləri

Azərbaycanca tərcümədəki haşiyə nömrələrinin (`parçalanma1`, `möminlər (1)`) qeydlərini Excel-də
doldurub `quran_translations_data.note`-a yazmaq üçün.

1. `python3 make_sheet.py` → `docs/quran/hasiye-qeydleri.xlsx` (hər nömrə bir sətir, səhifə sırası ilə;
   mövcud qeydlər yaşıl, boşlar sarı). Mətn serverdən (public REST) götürülür.
2. Faylda yalnız «Qeyd» sütununu doldur — «(1) -» hissəsini yazma, import özü qoyur.
3. `python3 import_notes.py` → `out/notes_import.sql` + dəyişiklik siyahısı + Oracle-da işlədən əmr.
   Yalnız serverdəkindən fərqlənən qeydlər yazılır; hər UPDATE köhnə qeydi şərt kimi daşıyır.

Nömrə qaydası tətbiqdəki `TextAnnotator.kt` ilə eynidir (`common.py`). Qeyd formatı: hər nömrə ayrı
sətirdə, `(N) - mətn`. Qeydlər redaktə növbəsindən (`quran_edits`) keçmir — birbaşa əsas cədvələ yazılır.
Lazımdır: `pip3 install --user openpyxl`.
