---
name: plan-status
description: iOS köçürmə planının cari vəziyyətini IOS_MIGRATION_PLAN.md faylının tamamını oxumadan çıxarır — sabitlənmiş "HAZIRDA HARDAYIQ" bloku, ən son dalğalar və qalan açıq checklist bəndləri. Sessiyaya başlayanda, "hardayıq / növbəti addım nədir" sualında və işi plana yazmazdan əvvəl işlət.
---

# /plan-status — planın cari vəziyyəti

## Niyə bu skill var

`IOS_MIGRATION_PLAN.md` **433 KB-dır** (~100k token). Tam oxumaq kontekst pəncərəsinin böyük
hissəsini yeyir. `## 🔖 HAZIRDA HARDAYIQ` bölməsi faylın kiçik bir hissəsi **deyil** — 409 KB-lıq
əsas gövdədir (23-cü sətirdən ~847-ə qədər).

Bölmənin quruluşu:
- **Başı** — sabitlənmiş cari vəziyyət bloku (ən son yazılan xülasə, cari faza).
- **Sonu** — xronoloji dalğa qeydlərinin ən yeniləri.
- **Ortası** — tarixçə. Konkret bir dalğa axtarmırsansa oxuma.

## İşlət

```bash
bash .claude/skills/plan-status/plan-status.sh
```

Skript üç şey verir: bölmənin başı (40 sətir), sonu (30 sətir) və bütün açıq `- [ ]` bəndləri
sətir nömrələri ilə.

## Nəticəni necə işlət

- Cari faza və növbəti addım üçün **başdakı blok** kifayətdir.
- Konkret bir keçmiş dalğanı axtarırsansa, faylı tam oxuma — `grep -n` ilə tarix və ya sinif adı
  üzrə tap, sonra `Read` ilə yalnız o `offset`-i oxu.
- Açıq bəndlərin çoxu Faza 7-dədir (Apple Developer / TestFlight / App Store) — bunlar istifadəçi
  qərarı tələb edir, kod işi deyil. Onları "növbəti addım" kimi təklif etmə.

## İş bitəndən sonra plana yazmaq

CLAUDE.md tələb edir: iş bitəndə `- [ ]` qutularını yenilə və status bölməsinə **tarixli** qeyd
əlavə et. Qaydalar:

- Yeni dalğa qeydi bölmənin **sonuna** yazılır (xronoloji ardıcıllıq pozulmasın).
- Cari vəziyyət dəyişibsə, bölmənin **başındakı** sabitlənmiş bloku da yenilə — köhnə qalarsa
  növbəti sessiya səhv yerdən başlayır.
- Tarix formatı: `(2026-07-28, Opus)` — mövcud qeydlərdəki formatı təkrarla.
- Sınanıb geri qaytarılan işi də yaz (`❌ Sınanıb geri qaytarılan:` bölməsi) — növbəti sessiya eyni
  divara ikinci dəfə çırpılmasın.
