# -*- coding: utf-8 -*-
"""GeoNames `alternateNamesV2` → `alt_names.tsv` (az/tr/ru axtarış aliasları).

`gen_cities.py` bu faylı oxuyur. Ayrıca saxlanılır, çünki mənbə dump **194 MB**-dır və hər dəfə
endirmək mənasızdır — nəticə isə bir neçə yüz KB.

    curl -O https://download.geonames.org/export/dump/alternateNamesV2.zip
    unzip -o alternateNamesV2.zip
    python3 gen_alt_names.py cities5000.txt

`cities5000` verilir, çünki o, `cities15000`-in **üst çoxluğudur** — tək alias faylı hər iki
kataloqa xidmət edir. Paketdəki siyahıya düşməyən gid-lər sadəcə heç vaxt oxunmur.

⚠️ Yalnız **az/tr/ru** götürülür və yalnız GÖSTƏRİLƏN ad kimi yox, AXTARIŞ ALİASI kimi işlədilir —
səbəbi `gen_cities.py`-dakı uzun şərhdədir (GeoNames-in `az` sahəsində Yevlax üçün «Gəncə» yazılıb).
Ərəb/kiril ekzonimləri qəsdən kənardadır: yoxlanmayıb və yanlış şəhərə aparır.

⚠️ Seçim qaydası mövcud `alt_names.tsv`-dən **geri qurulub** (4976/4976 sətir eyni çıxır):
`isPreferredName` üstündür, `isHistoric`/`isColloquial` atılır, qalan halda dump sırasında birinci.
Qaydanı dəyişsən mövcud aliaslar səssizcə sürüşər — «moskva» kimi işləyən sorğular itə bilər.
"""
import collections, sys

LANGS = ("az", "tr", "ru")
SOURCE = "alternateNamesV2.txt"


def wanted_gids(cities_path):
    """Dump-dakı bütün gid-lər. Süzgəc qəsdən yoxdur: `cities5000` onsuz da hər iki kataloqun
    mənbəyidir, ondan kənar milyonlarla obyekt üçün alias saxlamaq isə faylı 20 dəfə böyüdərdi."""
    gids = set()
    with open(cities_path, encoding="utf-8") as fh:
        for line in fh:
            c = line.split("\t", 1)
            if c[0].isdigit():
                gids.add(c[0])
    return gids


def collect(gids):
    """(gid, dil) → namizəd sətirlər, dump sırası qorunmuş."""
    cand = collections.defaultdict(list)
    with open(SOURCE, encoding="utf-8") as fh:
        for line in fh:
            c = line.rstrip("\n").split("\t")
            if len(c) < 4 or c[2] not in LANGS or c[1] not in gids:
                continue
            cand[(c[1], c[2])].append((
                c[3],
                len(c) > 4 and c[4] == "1",   # isPreferredName
                len(c) > 6 and c[6] == "1",   # isColloquial
                len(c) > 7 and c[7] == "1",   # isHistoric
            ))
    return cand


def pick(rows):
    # Danışıq dili və tarixi adlar atılır; hamısı belədirsə süzgəc ləğv olunur, yoxsa şəhər
    # ümumiyyətlə aliassız qalar (aliassız qalmaq «tapılmadı» deməkdir).
    pool = [r for r in rows if not (r[2] or r[3])] or rows
    for name, preferred, _, _ in pool:
        if preferred:
            return name
    return pool[0][0]


def main():
    cities = sys.argv[1] if len(sys.argv) > 1 else "cities15000.txt"
    gids = wanted_gids(cities)
    cand = collect(gids)

    rows = sorted((gid, lang, pick(v)) for (gid, lang), v in cand.items())
    with open("alt_names.tsv", "w", encoding="utf-8") as out:
        for gid, lang, name in rows:
            assert "\t" not in name and "\n" not in name, f"ad pozuq: {gid} {lang} {name!r}"
            out.write(f"{gid}\t{lang}\t{name}\n")

    covered = len({gid for gid, _, _ in rows})
    print(f"alt_names.tsv: {len(rows)} sətir, {covered}/{len(gids)} şəhər ({cities})")


if __name__ == "__main__":
    main()
