# `db-backup`

Məzmun yedəyinin **oxu qapısı**. Yedəyin özü Supabase-də saxlanmır — nə bucket, nə cron, nə jurnal.

## Rejimlər

| Gövdə | Cavab |
|---|---|
| `{"mode":"tables"}` | `backup_table_list()`-dən cədvəl siyahısı + hər birinin dəqiq sətir sayı (`Content-Range`) |
| `{"mode":"table","table":"hadith","offset":0,"limit":5000}` | həmin səhifənin JSON massivi |

Cavab **parse edilmir**: RPC-nin mətni olduğu kimi ötürülür — Edge Function limiti sorğu başına
2 s CPU-dur, 14 MB-lıq JSON-u JS-də açıb yenidən yığmaq həmin büdcəni yeyir.

## Kimlik

`verify_jwt = false`. Funksiya `x-backup-secret` başlığını Vault-dakı `backup_trigger_secret` ilə
tutuşdurur (`public.backup_secret_ok`).

⚠️ `Authorization` başlığı **qəsdən işlədilmir**: köhnə `service_role` JWT-ləri 2026-nın sonunda
dayanır, yeni `sb_secret_…` açarları isə `Authorization: Bearer`-də ümumiyyətlə qəbul olunmur.
Baza üçün açar `SUPABASE_SECRET_KEYS.default ?? SUPABASE_SERVICE_ROLE_KEY` kimi oxunur.

## Yedəyi kim alır

| Yer | Nə | Hara |
|---|---|---|
| **Mac** — `Yedək al.command` (iCloud qovluğunda, ikiqat klik) | bütün cədvəllər, hər biri ayrı fayl + `manifest.json`; son 30 qovluq saxlanılır | `~/Library/Mobile Documents/com~apple~CloudDocs/AnaMuslim-Yedekler/<tarix>/` |
| **Telefon** — ana ekrandakı xatırlatma / Ayarlar → Admin → «Məzmun yedəyi» | dua, əsma, hədis, tərcümə — **tək fayl** | sistem «hara saxlayım?» seçicisi |

⚠️ Telefon yolu bu funksiyaya **girmir**: `x-backup-secret` tətbiqə qoyula bilməz (repo açıqdır),
ona görə sorğular adminin öz Supabase sessiyası ilə gedir (`ContentBackupRepository`).

Quraşdırma: `./tools/mac/install-backup-button.sh` (sirr `~/.anamuslim-backup.env`-dən oxunur).

## Yerləşdirmə

MCP (`deploy_edge_function`, `verify_jwt: false`) və ya CLI:

```
supabase functions deploy db-backup --no-verify-jwt
```

## Əl ilə yoxlama

```
curl -sS -X POST "$SUPABASE_URL/functions/v1/db-backup" \
  -H "x-backup-secret: $BACKUP_SECRET" -H 'Content-Type: application/json' \
  -d '{"mode":"tables"}'
```

## Bərpa

Yedək faylı cədvəlin JSON massividir: `tools/supabase/restore-table.sh <tarix> <cədvəl>`
(default dry-run, service açarı yalnız həmin an mühit dəyişəni kimi verilir).
