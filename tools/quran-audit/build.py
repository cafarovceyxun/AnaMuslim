#!/usr/bin/env python3
"""Tərcümə yoxlanışını (docs/quran/TRANSLATION_AUDIT_2026-10-14.md) `quran_edits` təkliflərinə çevirir.

    python3 build.py prepare <az.json>   # notes.json, base.json, chunks/NN.json
    python3 site_ops.py <canvas> <az.json>  # saytın əl ilə yoxlanmış redaktələri → out/site.json
    python3 build.py merge   <az.json>   # sayt + manual/*.json + mexaniki → edits.json, seed_N.sql, verify.sql

`az.json` — `quran_translations_data` (slug `az`) dump-ı: [{id, chapter_no, verse_no, text, note}].
Bax README.md.
"""
import glob
import hashlib
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
AUDIT = os.path.join(ROOT, 'docs', 'quran', 'TRANSLATION_AUDIT_2026-10-14.md')
OUT = os.path.join(HERE, 'out')

# `quran_edits.editor_email` — heç bir hesabın e-poçtu deyil, ona görə `translations` view-u bu
# təklifləri heç kimin əsas mətninə qarışdırmır (coalesce yalnız öz e-poçtunla işləyir).
AUDIT_EDITOR = 'translation-audit'

REF = re.compile(r'(\d+):(\d+)(?:\s*[–-]\s*(?:(\d+):)?(\d+))?')
PREFIX = re.compile(r'^(​?)(\s*-?\s*)(\d+(?:\s*,\s*\d+)*)(\s*[:.]\s*)')


def load_rows(path):
    rows = json.load(open(path))
    return {(r['chapter_no'], r['verse_no']): r for r in rows}


def key(c, v):
    return f'{c}:{v}'


def expand_refs(cell, rows):
    """`2:30, 2:31`, `3:3–3:4`, `114:5/114:6`, `72:22–72:23` → [(c, v), …]. Uzun bloklar (≥4) boş qaytarır."""
    out = []
    for m in REF.finditer(cell):
        c, v = int(m.group(1)), int(m.group(2))
        if m.group(4):
            c2 = int(m.group(3)) if m.group(3) else c
            v2 = int(m.group(4))
            if c2 != c or v2 - v > 3:
                return []  # üslub blokları (17:1–17:59) mexaniki keçiddə həll olunur
            out += [(c, x) for x in range(v, v2 + 1) if (c, x) in rows]
        elif (c, v) in rows:
            out.append((c, v))
    return out


def strip_md(s):
    return re.sub(r'\*\*', '', s).strip()


def parse_notes(rows):
    lines = open(AUDIT, encoding='utf-8').read().split('\n')
    notes = {}
    section = None

    def add(refs, note):
        for c, v in refs:
            notes.setdefault(key(c, v), []).append(note)

    for ln in lines:
        if ln.startswith('## '):
            section = ln[3:].split('.')[0].strip()
            continue
        if section == '1' and ln.startswith('| ') and not ln.startswith('| Ayə') and not ln.startswith('|---'):
            cells = [x.strip() for x in ln.strip().strip('|').split('|')]
            if len(cells) < 3:
                continue
            refs = expand_refs(cells[0], rows)
            # `**MƏNA (orta — fail)** — izah`: növün özündə də tire ola bilər, ona görə qalın hissə ayrılır
            m = re.match(r'\*\*(.+?)\*\*\s*(?:\+[^—]*)?—\s*(.*)', cells[1])
            kind, prob = (m.group(1).strip(), m.group(2).strip()) if m else ('MƏNA', strip_md(cells[1]))
            add(refs, {'src': '§1', 'kind': kind, 'problem': strip_md(prob),
                       'suggestion': strip_md(cells[2])})
        elif section in ('3', '4') and ln.startswith('- ') and ln.count('|') >= 3:
            ref, kind, prob, sug = [x.strip() for x in ln[2:].split('|', 3)]
            if 'üslub' in kind and 'dırnaq' in kind:
                continue  # ASCII dırnaq blokları — mexaniki
            refs = expand_refs(ref, rows)
            add(refs, {'src': '§' + section, 'kind': kind, 'problem': prob, 'suggestion': sug})
    # §2-dəki ayə siyahılı sistemli qeydlər (söz əvəzləmələri mexaniki keçiddədir)
    systemic = [
        ('2:63, 2:93, 4:154, 19:52, 20:80, 28:29, 28:46', 'MƏNA (ad)',
         'الطُّور "bir dağ / dağın" kimi verilib — xüsusi addır', '"Tur dağı(nı/nın)"'),
        ('18:65, 18:71, 18:74', 'ad', '"(Xadir)" — الخضر', 'qəbul olunmuş forma "Xızır"'),
        ('5:1, 22:30', 'MƏNA (hökm)',
         'بَهِيمَةُ الْأَنْعَامِ / الْأَنْعَامُ "(digər) heyvanlar" kimi — yalnız dəvə, mal-qara, qoyun-keçidir',
         '"(digər) heyvanlar" yerinə "ən\'am heyvanları (dəvə, mal-qara, qoyun-keçi)"'),
        ('2:173, 16:115', 'MƏNA (hökm)',
         'meytənin izahı "(İslami kəsimlə kəsilməmiş heyvanlar)" hökmü genişləndirir — الْمَيْتَةَ kəsilmədən ölmüş heyvandır',
         '"(kəsilmədən ölmüş heyvan)"; 16:115-də yan-yana iki mötərizəni birləşdir'),
    ]
    for ref, kind, prob, sug in systemic:
        add(expand_refs(ref, rows), {'src': '§2', 'kind': kind, 'problem': prob, 'suggestion': sug})
    return notes


def parse_orphans():
    text = open(AUDIT, encoding='utf-8').read()
    block = text.split('## Əlavə B', 1)[1]
    return {(int(c), int(v)) for c, v in re.findall(r'(\d+):(\d+)', block)}


# ---------------------------------------------------------------- mexaniki keçid

WORDS = [
    # (regex, əvəz, səbəb)
    (r'\bdeyilmiydilər\b', 'deyildilərmi', 'deyilmiydi… → deyildimi…'),
    (r'\bdeyilmiydiniz\b', 'deyildinizmi', 'deyilmiydi… → deyildimi…'),
    (r'\bdeyilmiydik\b', 'deyildikmi', 'deyilmiydi… → deyildimi…'),
    (r'\bdeyilmiydin\b', 'deyildinmi', 'deyilmiydi… → deyildimi…'),
    (r'\bdeyilmiydi\b', 'deyildimi', 'deyilmiydi… → deyildimi…'),
    (r'\bfiron', 'Firon', 'Firon böyük hərflə'),
    (r'\b([Mm])unkər', lambda m: m.group(1) + 'ünkər', 'münkər'),
    (r'\b([Mm])utəkəbbir', lambda m: m.group(1) + 'ütəkəbbir', 'mütəkəbbir'),
    (r'\b([Mm])uşrik', lambda m: m.group(1) + 'üşrik', 'müşrik'),
    (r'\b([Mm])unəzzəh', lambda m: m.group(1) + 'ünəzzəh', 'münəzzəh'),
    (r'təkəbbürlülük', 'təkəbbürlük', 'təkəbbürlük'),
    (r'\bVahhəb', 'Vəhhab', 'Vəhhab'),
    (r'\bĞaffər', 'Ğaffar', 'Ğaffar'),
    (r'\bVaduddur', 'Vəduddur', 'Vədud'),
]

LETTER = 'A-Za-zƏəÖöÜüĞğIıİiŞşÇç'
KEEP_FOOTNOTE_DIGITS = True


def mechanical(text, orphan):
    """Mexaniki düzəlişlər — (yeni mətn, [səbəblər])."""
    reasons = []

    def sub(pattern, repl, reason, s, flags=0):
        new = re.sub(pattern, repl, s, flags=flags)
        if new != s and reason not in reasons:
            reasons.append(reason)
        return new

    m = PREFIX.match(text)
    if m:
        zw, dash, nums, sep = m.groups()
        body = text[m.end():]
        std = '-' + re.sub(r'\s*,\s*', ', ', nums) + ': '
        if text[:m.end()] != std:
            reasons.append('ayə prefiksi standart formaya salındı')
        text = std + body
    head = PREFIX.match(text)
    pre, body = (text[:head.end()], text[head.end():]) if head else ('', text)

    # 2026-10-18: haşiyə nömrələri qalır (qeydləri sonra yazılacaq, tətbiq üst-indeks göstərir) —
    # bu addım söndürülüb, gözləyən təkliflərdə isə restore_footnotes.py ilə geri alınıb.
    if orphan and not KEEP_FOOTNOTE_DIGITS:
        body = sub(rf'(?<=[{LETTER}),.!?;:”’])\d+(?=[\s.,;:!?)”…(]|$)', '', 'yetim dipnot rəqəmi silindi', body)
    for pat, repl, reason in WORDS:
        body = sub(pat, repl, reason, body)
    body = sub(r'"', '”', 'dırnaq vahid simvola (”) salındı', body)
    body = sub(r'“', '”', 'dırnaq vahid simvola (”) salındı', body)
    body = sub(rf"(?<=[{LETTER}])'(?=[{LETTER}])", '’', 'apostrof ’ simvoluna salındı', body)
    body = sub(r'(?<=\S) {2,}(?=\S)', ' ', 'artıq boşluq', body)
    body = sub(r'\(\s+', '(', 'mötərizə daxilində boşluq', body)
    body = sub(r'(?<=\S)\s+\)', ')', 'mötərizə daxilində boşluq', body)
    body = sub(r'(?<=[\w)”])\s+([:;,!?])(?!\.)', r'\1', 'durğu işarəsindən əvvəl boşluq', body)
    body = sub(rf'(?<=[{LETTER}])([!?.;])(?=[{LETTER}]{{2}})', r'\1 ', 'durğu işarəsindən sonra boşluq', body)
    return pre + body, reasons


# ---------------------------------------------------------------- prepare / merge

def prepare(dump):
    rows = load_rows(dump)
    notes = parse_notes(rows)
    orphans = parse_orphans()
    os.makedirs(os.path.join(OUT, 'chunks'), exist_ok=True)
    base = {}
    for (c, v), r in sorted(rows.items()):
        k = key(c, v)
        text, reasons = mechanical(r['text'], (c, v) in orphans)
        if reasons or k in notes:
            base[k] = {'id': r['id'], 'original': r['text'], 'text': text, 'mechanical': reasons}
    json.dump(notes, open(os.path.join(OUT, 'notes.json'), 'w'), ensure_ascii=False, indent=1)
    json.dump(base, open(os.path.join(OUT, 'base.json'), 'w'), ensure_ascii=False, indent=1)

    manual = [k for k in base if k in notes]
    manual.sort(key=lambda k: tuple(map(int, k.split(':'))))
    n_chunks = 6
    size = -(-len(manual) // n_chunks)
    for i in range(n_chunks):
        part = manual[i * size:(i + 1) * size]
        payload = [{'ref': k, 'text': base[k]['text'], 'notes': notes[k]} for k in part]
        json.dump(payload, open(os.path.join(OUT, 'chunks', f'{i + 1:02}.json'), 'w'),
                  ensure_ascii=False, indent=1)
    print(f'notes: {len(notes)} ayə, mexaniki: {sum(1 for b in base.values() if b["mechanical"])} ayə, '
          f'cəmi: {len(base)} ayə, əl ilə: {len(manual)} ({n_chunks} hissə)')


def reason_lines(notes):
    lines, seen = [], set()
    for n in notes:
        if n['src'] == '§1' and any(x['src'] != '§1' for x in notes):
            continue  # §1 cədvəli §3/§4-ün təkrarıdır
        sig = (n['kind'], n['problem'])
        if sig not in seen:
            seen.add(sig)
            lines.append(f"• {n['kind']}: {n['problem']}")
    return lines


def sql_str(s):
    return "'" + s.replace("'", "''") + "'"


def sql_or_null(s):
    return 'null' if s is None else sql_str(s)


def merge(dump):
    """Mənbə sırası: sayt redaktəsi (əl ilə yoxlanıb) → agent nəticəsi (manual/) → yalnız mexaniki.

    Sayt redaktələri **orijinal** mətnə yazılıb, ona görə mexaniki keçid onların üstündən təkrar
    işlədilir; agent nəticələri isə artıq mexaniki mətndən qurulub.
    """
    rows = load_rows(dump)
    base = json.load(open(os.path.join(OUT, 'base.json')))
    notes = json.load(open(os.path.join(OUT, 'notes.json')))
    site = json.load(open(os.path.join(OUT, 'site.json')))['verses']
    orphans = parse_orphans()
    # Mötərizə qaydası düzəlişləri, sırası ilə: təkliflərin öz mötərizələri, sonra müəllifinkilər (audit §5)
    layers = []
    for stem, fname in (('paren_fix', 'paren_fixes.json'), ('author_fix', 'author_fixes.json')):
        raw = json.load(open(os.path.join(HERE, fname)))
        layers.append((stem, {k: (x['edits'] if 'edits' in x else [[x['from'], x['to']]], x['reason'])
                              for k, x in raw.items()}))
    patched = {stem: [] for stem, _ in layers}
    manual = {}
    for f in sorted(glob.glob(os.path.join(OUT, 'manual', '*.json'))):
        for item in json.load(open(f)):
            manual[item['ref']] = item
    refs = set(base) | {k for k in site if '#' not in k} | {k for _, fx in layers for k in fx}
    edits, unresolved, sources = [], [], {'sayt': 0, 'agent': 0, 'mexaniki': 0}
    for k in sorted(refs, key=lambda k: tuple(map(int, k.split(':')))):
        c, v = map(int, k.split(':'))
        row = rows[(c, v)]
        original = row['text']
        if k in base and base[k]['original'] != original:
            sys.exit(f'{k}: serverdəki mətn dəyişib — prepare-i yenidən işlət')
        lines = []
        if k in site:
            text, mech = mechanical(site[k]['text'], (c, v) in orphans)
            lines = ['• ' + r for r in site[k]['reasons']]
            sources['sayt'] += 1
        elif k in notes:
            m = manual.get(k)
            if m is None:
                unresolved.append(k)
                continue
            text, mech = m['text'], base[k]['mechanical']
            lines = reason_lines(notes[k])
            if m.get('skipped'):
                lines.append('• tətbiq olunmayıb: ' + '; '.join(m['skipped']))
            sources['agent'] += 1
        elif k in base:
            text, mech = base[k]['text'], base[k]['mechanical']
            sources['mexaniki'] += 1
        else:
            text, mech = original, []  # yalnız mötərizə düzəlişi olan ayə
        if mech:
            lines.append('• mexaniki: ' + '; '.join(mech))
        note = site.get(k + '#note', {}).get('text')
        for stem, fx in layers:
            if k not in fx:
                continue
            pairs, why = fx[k]
            before = text
            for a, b in pairs:
                if text.count(a) != 1:
                    sys.exit(f'{k}: {stem} — "{a}" mətndə tapılmadı (və ya təkrarlanır), mənbə dəyişib')
                text = text.replace(a, b)
            lines.append('• mötərizə: ' + why)
            # əvvəlki qatlarda artıq təklif idisə — serverdə update, deyilsə — yeni insert
            patched[stem].append({'id': row['id'], 'c': c, 'v': v, 'original': original, 'before': before,
                                  'text': text, 'line': lines[-1], 'existed': before != original or note is not None})
        if text == original and note is None:
            continue
        edits.append({'ref': k, 'translation_id': row['id'], 'chapter_no': c, 'verse_no': v,
                      'original': original, 'new_text': text, 'note': note, 'reason': '\n'.join(lines)})
    if unresolved:
        sys.exit(f'manual/ cavabı olmayan {len(unresolved)} ayə: {unresolved[:10]}…')
    json.dump(edits, open(os.path.join(OUT, 'edits.json'), 'w'), ensure_ascii=False, indent=1)

    for f in glob.glob(os.path.join(OUT, 'seed_*.sql')):
        os.remove(f)
    per = 150
    for i in range(0, len(edits), per):
        vals = ',\n'.join(
            f"({e['translation_id']}, {e['chapter_no']}, {e['verse_no']}, {sql_str(e['new_text'])}, "
            f"{sql_or_null(e['note'])}, {sql_str(e['reason'])}, md5({sql_str(e['original'])}))"
            for e in edits[i:i + per])
        sql = f"""-- tərcümə yoxlanışı təklifləri {i + 1}–{min(i + per, len(edits))} / {len(edits)}
-- Yalnız serverdəki mətn yoxlanış anındakı ilə eynidirsə yazılır (md5).
insert into public.quran_edits (translation_id, chapter_no, verse_no, new_text, note, reason, editor_email, user_id)
select v.id, v.c, v.v, v.new_text, v.note, v.reason, '{AUDIT_EDITOR}', null
  from (values
{vals}
  ) as v(id, c, v, new_text, note, reason, orig_md5)
  join public.quran_translations_data q on q.id = v.id and md5(q.text) = v.orig_md5
 where not exists (select 1 from public.quran_edits e
                    where e.translation_id = v.id and e.editor_email = '{AUDIT_EDITOR}' and not e.is_approved);
"""
        open(os.path.join(OUT, f'seed_{i // per + 1}.sql'), 'w').write(sql)
    agg = hashlib.md5(''.join(e['new_text'] for e in sorted(edits, key=lambda e: e['translation_id']))
                      .encode()).hexdigest()
    open(os.path.join(OUT, 'verify.sql'), 'w').write(
        f"""-- gözlənilən: {len(edits)} sətir, md5 {agg}
select count(*), md5(string_agg(new_text, '' order by translation_id))
  from public.quran_edits where editor_email = '{AUDIT_EDITOR}' and not is_approved;
""")
    # Artıq yazılmış (seed) təkliflər üçün: yalnız mətn mötərizə düzəlişindən əvvəlki kimidirsə dəyişir
    # (admin «Redaktə» ilə toxunubsa, md5 tutmur və sətir olduğu kimi qalır).
    full_reason = {e['translation_id']: e['reason'] for e in edits}
    for stem, items in patched.items():
        upd = [f"update public.quran_edits set new_text = {sql_str(x['text'])}, reason = reason || {sql_str(chr(10) + x['line'])}\n"
               f" where translation_id = {x['id']} and editor_email = '{AUDIT_EDITOR}' and not is_approved"
               f" and md5(new_text) = md5({sql_str(x['before'])});"
               for x in items if x['existed']]
        new = [x for x in items if not x['existed']]
        ins = ''
        if new:
            vals = ',\n'.join(f"({x['id']}, {x['c']}, {x['v']}, {sql_str(x['text'])}, {sql_str(full_reason[x['id']])}, "
                              f"md5({sql_str(x['original'])}))" for x in new)
            ins = f"""insert into public.quran_edits (translation_id, chapter_no, verse_no, new_text, reason, editor_email, user_id)
select v.id, v.c, v.v, v.new_text, v.reason, '{AUDIT_EDITOR}', null
  from (values
{vals}
  ) as v(id, c, v, new_text, reason, orig_md5)
  join public.quran_translations_data q on q.id = v.id and md5(q.text) = v.orig_md5
 where not exists (select 1 from public.quran_edits e
                    where e.translation_id = v.id and e.editor_email = '{AUDIT_EDITOR}' and not e.is_approved);
"""
        open(os.path.join(OUT, f'{stem}.sql'), 'w').write(
            f'-- mötərizə qaydası (README): {len(upd)} təklif yenilənir, {len(new)} yeni təklif.'
            f' Bir tranzaksiyada işlət, sonra verify.sql.\n'
            f'begin;\n' + '\n'.join(upd) + '\n' + ins + 'commit;\n')
        print(f'{stem}.sql: {len(upd)} update, {len(new)} insert')
    print(f'{len(edits)} təklif ({sources}), {len(glob.glob(os.path.join(OUT, "seed_*.sql")))} seed faylı, '
          f'md5 {agg}')


# ---------------------------------------------------------------- mötərizə qaydası

GROUP = re.compile(r'\([^()]*\)')


def check_parens(_=None):
    """Təklifin ƏLAVƏ etdiyi hər mötərizəni iki oxunuşda göstərir və qırılanları işarələyir.

    Qayda: mötərizə təfsirdir — mətn həm mötərizə ilə (mötərizə işarələri atılır), həm də mötərizəsiz
    (mötərizə məzmunu ilə birlikdə atılır) düzgün cümlə olmalıdır. Skript yalnız formal izləri tutur
    (kiçik hərflə başlama, ",." kimi qalıq durğu, sözə yapışıq mötərizə); məna yoxlaması gözlə olur —
    `--all` bütün yeni mötərizələri hər iki oxunuşla çap edir.
    """
    show_all = '--all' in sys.argv
    edits = json.load(open(os.path.join(OUT, 'edits.json')))
    without = lambda s: re.sub(r'\s*\([^()]*\)', '', s)
    flagged = 0
    for e in edits:
        orig, new = e['original'], e['new_text']
        left = {}
        for g in GROUP.findall(orig):
            left[g] = left.get(g, 0) + 1
        added = []
        for g in GROUP.findall(new):
            if left.get(g):
                left[g] -= 1
            else:
                added.append(g)
        if not added:
            continue
        bare = without(new)
        issues = []
        # müəllifin öz mötərizələrindən qalan izlər sayılmır — yalnız təklifin artırdığı
        for name, pat in (('kiçik hərflə başlayır', r'(?:^\s*-?\d+:\s*|[.!?]\s+|[:.!?]?”\s*)[a-zəöüğıişç]'),
                          ('qalıq durğu', r'[,;:]\s*[,.;!?]|\s[,.;!?]')):
            if len(re.findall(pat, bare)) > len(re.findall(pat, without(orig))):
                issues.append(name)
        for g in added:
            i = new.index(g)
            if (i and new[i - 1].isalpha()) or new[i + len(g):i + len(g) + 1].isalpha():
                issues.append(f'yapışıq {g}')
        if issues or show_all:
            flagged += bool(issues)
            print(f"{e['ref']}  {'; '.join(issues) or 'ok'}\n  + {re.sub(r'[()]', '', new).strip()}\n  - {bare.strip()}")
    print(f'\n{flagged} təklifdə formal iz var (məna yoxlaması üçün: --all)')


if __name__ == '__main__':
    {'prepare': prepare, 'merge': merge, 'check-parens': check_parens}[sys.argv[1]](*sys.argv[2:3])
