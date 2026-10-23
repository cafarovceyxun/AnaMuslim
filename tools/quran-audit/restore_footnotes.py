"""Gözləyən yoxlanış təkliflərində «yetim dipnot rəqəmi silindi» addımını geri alır (2026-10-18).

Qərar dəyişdi: haşiyə nömrələri qalır (qeydləri sonra yazılacaq), tətbiq onları üst-indeks göstərir
(`TextAnnotator.kt` → `findFootnoteMarkers`). Mexaniki keçid isə nömrələri təkliflərin mətnindən
silmişdi və təkliflərin üstündən artıq mötərizə/müəllif düzəlişləri keçib — ona görə `build.py`-ı
yenidən işlətmək olmaz, nömrə hər təklifdə öz yerinə qaytarılır.

    python3 restore_footnotes.py <pending.json>      # hesabat + out/restore_footnotes.sql

`pending.json` — serverdən (read only) çəkilmiş `json_agg`: eid, tid, ref, base, note, new, reason:
    select json_agg(json_build_object('eid', qe.id, 'tid', t.id, 'ref', t.chapter_no||':'||t.verse_no,
      'base', t.text, 'note', t.note, 'new', qe.new_text, 'reason', qe.reason))
    from quran_edits qe join quran_translations_data t on t.id = qe.translation_id
    where not qe.is_approved and qe.editor_email = 'translation-audit';
Yer: əsas mətndə nömrə silinmiş mətnlə təklif `difflib` ilə tutuşdurulur; nömrənin solundakı simvol
dəyişməmiş blokdadırsa həmin yerə qayıdır, yoxsa ayə əl ilə baxmaq üçün siyahıya düşür.
SQL yalnız təklif ixrac anındakı kimidirsə (md5) yazır; nömrə qayıdandan sonra əsas mətnlə eyni
qalan təklif (başqa düzəlişi yox idi) silinir.
"""
import difflib
import hashlib
import json
import os
import re
import sys

from build import LETTER, PREFIX

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out')
# build.py-dakı yetim dipnot ifadəsinin özü — silinən hər şeyi, yalnız onu tapır
MARKER = re.compile(rf'(?<=[{LETTER}),.!?;:”’])\d+(?=[\s.,;:!?)”…(]|$)')
REASON = 'yetim dipnot rəqəmi silindi'
# serverdəki təklifdə söz ikiləşib (2026-10-18-də tapıldı) — düzəliş nömrəni saxlayır, artıq vergülü yox
FIXES = {'6:104': ('dəlillər1dəlillər gəlmişdir', 'dəlillər1 gəlmişdir')}


def split_prefix(text):
    m = PREFIX.match(text)
    return (text[:m.end()], text[m.end():]) if m else ('', text)


def restore(base, new):
    """(bərpa olunmuş mətn, qaytarılan nömrə sayı, yeri tapılmayan nömrələr)."""
    _, base_body = split_prefix(base)
    new_pre, new_body = split_prefix(new)

    # əsas mətndən nömrələri sil, hər nömrənin silinmiş mətndəki yerini yadda saxla
    stripped, inserts, last = [], [], 0
    for m in MARKER.finditer(base_body):
        stripped.append(base_body[last:m.start()])
        inserts.append((sum(map(len, stripped)), m.group()))
        last = m.end()
    stripped.append(base_body[last:])
    stripped = ''.join(stripped)
    if not inserts:
        return new, 0, []

    blocks = difflib.SequenceMatcher(None, stripped, new_body, autojunk=False).get_matching_blocks()
    placed, missed = [], []
    for pos, digits in inserts:
        # nömrənin solundakı simvol (pos-1) dəyişməmiş blokda olmalıdır
        target = next((b.b + (pos - b.a) for b in blocks if b.size and b.a < pos <= b.a + b.size), None)
        if target is None or new_body[target:target + len(digits)] == digits:
            if target is None:
                ctx = stripped[max(0, pos - 25):pos]
                missed.append(f'…{ctx}[{digits}]{stripped[pos:pos + 10]}…')
            continue
        placed.append((target, digits))

    body = new_body
    for target, digits in sorted(placed, reverse=True):
        body = body[:target] + digits + body[target:]
    return new_pre + body, len(placed), missed


def clean_reason(reason):
    lines = []
    for ln in (reason or '').split('\n'):
        if REASON in ln and ln.startswith('• mexaniki:'):
            items = [x.strip() for x in ln[len('• mexaniki:'):].split(';') if REASON not in x]
            if items:
                lines.append('• mexaniki: ' + '; '.join(items))
        elif 'yetim dipnot' in ln:
            continue
        else:
            lines.append(ln)
    return '\n'.join(lines).strip()


def dollar(s):
    tag = 'q'
    while f'${tag}$' in s:
        tag += 'q'
    return f'${tag}${s}${tag}$'


def main(path):
    # psql çıxışında BEGIN/ROLLBACK sətirləri də olur — JSON tək sətirdir
    rows = json.loads(next(ln for ln in open(path, encoding='utf-8') if ln.startswith('[')))
    updates, deletes, manual, restored = [], [], [], 0
    for r in rows:
        if 'yetim dipnot' not in (r['reason'] or ''):
            continue
        text, n, missed = restore(r['base'], r['new'])
        if r['ref'] in FIXES:
            old, fixed = FIXES[r['ref']]
            if old not in text:
                sys.exit(f"{r['ref']}: düzəliş tapılmadı — təklif dəyişib, FIXES-ə yenidən bax")
            text, n = text.replace(old, fixed), n + 1
        if missed:
            manual.append((r['ref'], missed))
        if not n:
            continue
        restored += n
        md5_old = hashlib.md5(r['new'].encode()).hexdigest()
        if text == r['base']:
            deletes.append((r, md5_old))
        else:
            updates.append((r, text, clean_reason(r['reason']), md5_old))

    sql = ['-- restore_footnotes.py: gözləyən təkliflərə haşiyə nömrələrini qaytarır', 'begin;']
    for r, text, reason, md5_old in updates:
        sql.append(f"update quran_edits set new_text = {dollar(text)}, reason = {dollar(reason)} "
                   f"where id = {r['eid']} and not is_approved and md5(new_text) = '{md5_old}';")
    for r, md5_old in deletes:
        sql.append(f"delete from quran_edits where id = {r['eid']} and not is_approved "
                   f"and md5(new_text) = '{md5_old}';")
    sql.append('commit;')
    out = os.path.join(OUT, 'restore_footnotes.sql')
    open(out, 'w', encoding='utf-8').write('\n'.join(sql) + '\n')

    print(f'yenilənəcək təklif: {len(updates)}, silinəcək (yalnız nömrə silinməsi idi): {len(deletes)}, '
          f'qaytarılan nömrə: {restored}')
    print(f'əl ilə baxılacaq ayə: {len(manual)}')
    for ref, missed in manual:
        for m in missed:
            print(f'  {ref}: {m}')
    print(f'SQL: {out}')


if __name__ == '__main__':
    main(sys.argv[1])
