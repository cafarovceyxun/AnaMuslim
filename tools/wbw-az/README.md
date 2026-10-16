# Azərbaycanca sözbəsöz (WBW) — 2026-10-15

Orijinal layihənin (AlfaazPlus) sözbəsöz paketlərində azərbaycanca yoxdur. Bu qovluq onu **öz serverimizdə**
saxlayır və tətbiqə orijinal paketlərlə eyni formatda verir.

| Nə | Harada |
|---|---|
| Əsas mənbə | `public.quran_wbw_az` (Oracle, `migration.sql`) — hər söz bir sətir, 77 429 söz |
| Yedək / təkrar qurmaq üçün | `data/glosses.json.gz` (`assemble`/`merge` yazır) |
| Tətbiqə gedən paket | storage `wbw/wbw_az.json.gz` (public) |
| Öz manifest | storage `wbw/available_wbw_info_own.json` → `ApiConfig.OWN_WBW_MANIFEST_URL` |
| Tətbiqdə | `GithubApi.getAvailableWbwInfo()` orijinal siyahı ilə öz siyahını birləşdirir; `OFFERED_WBW_LANG_CODES`-də `az` birincidir |

Mənalar ərəbcə sözə (Osmani yazı, `quranapp.db` `ayah_words`, `script_id = 1`) görə yazılıb; terminologiya
Mürşüd Yusifoğlunun tərcüməsindəndir, türkcə/ingiliscə sözbəsöz yalnız köməkdir. Qaydalar `GUIDE.md`-dədir
(qısa, təfsirsiz, mötərizə yalnız zərurətdə və iki oxunuşda da düz). Transliterasiya ingiliscə paketdəndir.
Mətnlər süni intellektlə hazırlanıb (Sonnet agentləri, `GUIDE.md` + `validate.py`) — sınaq versiyasıdır.

## Tərcümə dəyişəndə (audit düzəlişləri təsdiqlənəndə)

Trigger `on_quran_translation_changed_wbw` həmin ayənin sözlərini `status = 'stale'` edir. Sonra:

```
python3 publish.py stale <iş-qovluğu>          # stale ayələr → <iş>/in/stale.txt
# agentə ver: GUIDE.md + in/stale.txt → out/stale_a.json …; yoxla:
WBW_WORK=<iş-qovluğu> python3 validate.py stale
python3 publish.py merge <iş-qovluğu>/out      # data/glosses.json.gz yenilənir
python3 publish.py load                        # cədvələ (status yenə 'ok')
python3 publish.py pack                        # versiya +1
python3 publish.py upload                      # əvvəl paket, sonra manifest
```

Tətbiq versiyanın artdığını manifestdən görür və «yeniləmə var» göstərir — mağaza buraxılışı lazım deyil.
Gözləyən sözlərə bax: `select chapter_no, verse_no from quran_wbw_az where status = 'stale' group by 1, 2;`

## İlk qurulma

```
python3 publish.py assemble <out-qovluğu> <az.json>   # bütün ayələr, söz sayı ərəbcə ilə tam uyğun
python3 publish.py load && python3 publish.py pack && python3 publish.py upload
```

`az.json` — `quran_translations_data` (slug `az`) dump-ı; `source_md5` ondan hesablanır.

⚠️ `upload` service_role açarını yalnız serverdə `/opt/supabase/.env`-dən oxuyur, çap etmir. Kong host-a açıq
deyil, ona görə yükləmə serverdən ictimai HTTPS ünvanına gedir. Fayl ötürmə `scp` ilə yox, `ssh … 'cat > …'`
ilədir (sandbox-da `scp` açarı oxumur).

Ünvan dəyişsə (məs. storage başqa yerə köçsə) `ApiConfig.OWN_WBW_MANIFEST_URL` və `publish.py`-dakı `PUBLIC`
birlikdə dəyişməlidir — köhnə buraxılışlar köhnə ünvanı oxumağa davam edir.
