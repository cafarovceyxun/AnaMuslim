import json, sqlite3, os
exec(open('verses.py').read())
B = os.path.expanduser('~/Library/Mobile Documents/com~apple~CloudDocs/anamuslim-mezmun-2026-10-03.json')
T = {(r['chapter_no'], r['verse_no']): r for r in json.load(open(B))['tables']['quran_translations_data']}
db = sqlite3.connect('../../../app/src/main/assets/db/quranapp.db')
def words(c, v):
    return [w for (w,) in db.execute("select text from ayah_words w join ayahs a using(ayah_id) where script_id=1 and surah_no=? and ayah_no=? order by word_index", (c, v))]
for sub, name in SUBS:
    print(f'\n######## {sub}')
    for c, a, b in V[sub]:
        for v in range(a, (b or a) + 1):
            r = T[(c, v)]
            w = words(c, v)
            print(f'[{c}:{v}] ' + ' '.join(f'{i}:{x}' for i, x in enumerate(w)))
            print('   AZ: ' + r['text'] + (('  ||NOTE: ' + r['note'][:200]) if r['note'] else ''))
