# Tərcümə yoxlanışı → `quran_edits` təklifləri (2026-10-14)

Mənbə: `docs/quran/TRANSLATION_AUDIT_2026-10-14.md`. Hər düzəliş `quran_edits`-ə **gözləyən** təklif kimi
yazılır (`editor_email = 'translation-audit'`, `user_id = null`, səbəb `reason` sütununda) və adi düzəlişlər
kimi Ayarlar → Düzəlişləri İdarə Et-də təsdiqlənir. Heç bir hesabın e-poçtu olmadığı üçün `translations`
view-u bu mətnləri heç kimin əsas tərcüməsinə qarışdırmır.

Giriş etmiş istifadəçi eyni təklifləri **ikinci tərcümə** kimi görür: kataloqdakı `az-review` kitabı
(`source_column = 'text_review'`, `is_public = false`) view-un `text_review`/`note_review` sütunlarından gəlir —
yalnız təklifi olan ayələr. Oxucuda üst paneldəki və ayənin altındakı düymə yarımçıq vərəqi açır
(`TranslationReviewSheet.kt`): ‹ › təkliflər arasında gəzir, oxucu hər addımda həmin ayəyə sürüşür,
«Redaktə» təklifi dəyişir (admin; redaktorun mətni onun öz düzəlişi kimi növbəyə düşür).

## Qurulma

```
python3 build.py prepare <az.json>            # notes.json, base.json (mexaniki keçid), chunks/NN.json
python3 site_ops.py <canvas> <az.json>        # duzelis saytının əl ilə yoxlanmış redaktələri → site.json
python3 build.py merge <az.json>              # edits.json, seed_N.sql, verify.sql
python3 diff.py edits 2:36 11:40              # fərqə bax
python3 build.py check-parens [--all]         # mötərizə qaydası (aşağıda)
python3 author_parens.py <az.json> [--list]   # eyni qayda müəllifin ÖZ mötərizələrinə (audit §5)
```

### Mötərizə qaydası

Mötərizə təfsirdir: təklif olunan mətn **həm mötərizə ilə, həm də mötərizəsiz** düzgün cümlə olmalıdır.
Ərəbcədə olan söz (fa → «elə isə», الَّذِي → «o … ki», feildəki «siz», ikilik mübtədası) mötərizəyə girmir;
«ki», vergül və cümlə başındakı böyük hərf mötərizədən kənarda qalır; vergüllü əlavə `(, …)` yazılır.
`check-parens` yalnız təklifin **artırdığı** mötərizələrə baxır və formal izləri tutur (kiçik hərflə
başlama, qalıq durğu, sözə yapışıq mötərizə) — məna üçün `--all` çıxışını gözlə oxu. Düzəlişlər
`paren_fixes.json`-dadır (`from` → `to`, mənbə seçiləndən və mexaniki keçiddən **sonra** tətbiq olunur;
`from` tapılmasa `merge` dayanır). Müəllifin öz mötərizələrinə bu keçid toxunmur — onları `author_parens.py` sayır (formal izlər; kiçik hərflə başlama üslub kimi ayrıca), məna/qrammatika qırılmaları əl ilə ərəbcə ilə tutuşdurulub audit sənədinin §5-inə yazılıb. Bu tapıntıların təklifləri `author_fixes.json`-dadır (hər ayə üçün `edits: [[from, to], …]` + səbəb; toplu iki nöqtə/durğu cütləri qayda ilə qurulub) və `paren_fixes.json`-dan sonra tətbiq olunur — 157 ayə, 2026-10-14-də serverə yazılıb (61 update, 96 yeni təklif, cəmi 1164).

- `az.json` — `quran_translations_data` (slug `az`) dump-ı; açıq REST-dən də çəkmək olar (anon oxuyur).
- Mənbə sırası (`merge`): **sayt redaktəsi** (`duzelis.cafarovceyxun.com`-un generatoru, sessiya `3ad0c114…`
  scratchpad-ında `canvas/` — `manual.py`/`manual2.py` istifadəçi ilə yoxlanıb) → saytda olmayan ayələr üçün
  **agent nəticəsi** (`out/manual/NN.json`, `chunks/`-dan Sonnet ilə) → üstündən **mexaniki keçid**
  (yetim dipnot rəqəmləri, ayə prefiksi, `) :` boşluqları, ASCII dırnaq → ”, `deyilmiydi` → `deyildimi`,
  `firon` → `Firon`, `munkər`/`mutəkəbbir`/…, `Vahhəb`/`Ğaffər`/`Vaduddur`).
- Tətbiq olunmayanlar: səcdə işarəsi ۩ (qərar verilməyib), «Qaviy/Ğaniy/Afuv — bir forma seçin», «Xabir → Xəbir».

## Server

1. `migration.sql` (bir tranzaksiya): `quran_edits.reason`, oxu siyasəti `quran_edits_select_review`,
   kataloq CHECK-inə `text_review`, view-a iki sütun **sonda** (`create or replace`), `az-review` kitabı.
   Əvvəl `select pg_get_viewdef('public.translations', true);` ilə view-un indiki tərifini tutuşdur.
2. `seed_N.sql` — hər fayl yalnız serverdəki mətn yoxlanış anındakı ilə eynidirsə (md5) yazır və təkrar
   işlədiləndə ikiləmir.
3. `paren_fix.sql`, sonra `author_fix.sql` — seed **artıq yazılıbsa**: mötərizə düzəlişlərini gözləyən təkliflərə
   tətbiq edir (yalnız mətn düzəlişdən əvvəlki kimidirsə — admin «Redaktə» ilə toxunubsa, sətir qalır), təklifi
   olmayan ayələr üçün yeni təklif yazır. Hər ikisi 2026-10-14-də işlədilib.
4. `verify.sql` — sətir sayı + md5 `merge`-in çapı ilə eyni olmalıdır.

Geri almaq: `delete from quran_edits where editor_email = 'translation-audit' and not is_approved;`

### Haşiyə nömrələri qalır (2026-10-18)

Mexaniki keçid sözə yapışıq «yetim» haşiyə nömrələrini (`parçalanma1`) silirdi. Qərar dəyişdi: nömrələr qalır,
qeydləri sonra yazılacaq, tətbiq onları üst-indeks göstərir. `build.py`-da addım söndürülüb
(`KEEP_FOOTNOTE_DIGITS`), artıq yazılmış təkliflər üçün:

```
python3 restore_footnotes.py <pending.json>   # out/restore_footnotes.sql + hesabat
```

`pending.json` serverdən read-only `json_agg`-dir (sorğu skriptin başındadır). Nömrə `difflib` ilə öz sözünün
arxasına qayıdır; SQL yalnız md5 uyğun gələn təklifə yazır, nömrə qayıdandan sonra əsas mətnlə eyni qalan təklifi
(yeganə dəyişikliyi nömrə silmək idi) silir. 6:104 təklifində söz ikiləşmişdi (`dəlillər1dəlillər`) — skriptdə
`FIXES` ilə düzəlir.

**2026-10-20-də Oracle-da tətbiq olundu:** təzə ixracla (arada 46 təklif emal olunmuşdu, əsas mətnləri dəyişməmişdi)
42 təklif yeniləndi, 160 silindi (gözləyən 1072 → 912), bir tranzaksiyada md5 + say yoxlaması ilə. İxrac nüsxələri
`out/pending_backup_2026-10-18.json` / `-10-20.json`. Nömrəni mexaniki addım yox, **əl düzəlişinin içində** silən 7
təklif (4:85, 4:128, 11:87, 21:89, 55:5, 56:55, 58:2) üçün `out/restore_footnotes_manual.sql` — mətnə nömrə,
səbəbdən «yetim rəqəm» ifadəsi — eyni gün ayrıca tətbiq olundu. Yoxlama: gözləyən təkliflərin heç birində əsas mətndən
az nömrə yoxdur; 6:104-ün səbəbindən də «yetim rəqəm» ifadəsi çıxarıldı (yalnız `reason`).

## Sayt (duzelis.cafarovceyxun.com)

`python3 site_from_db.py deploy` — gözləyən təklifləri birbaşa `quran_edits`-dən çəkir (bir ayə = bir kart: hazırkı
mətn ↔ təklif, səbəb, ərəbcə), `site/template.html` şablonu ilə qurur, serverdəki səhifəni yedəkləyib əvəzləyir.
**Avtomatikdir (2026-10-24):** serverdə `duzelis-refresh.timer` hər 5 dəqiqədən bir `/opt/duzelis/site_from_db.py
refresh` işlədir — təkliflər dəyişməyibsə heç nə yazmır, dəyişibsə səhifəni yenidən qurur. Brauzer cədvəli birbaşa
oxuya bilmir (`quran_edits` yalnız daxil olmuşlara açıqdır). Skripti və ya şablonu dəyişəndə `python3 site_from_db.py
install` ilə serverdəki nüsxəni yenilə. `build` yalnız `site/out/index.html` yazır. Söndürmək:
`sudo systemctl disable --now duzelis-refresh.timer`.
