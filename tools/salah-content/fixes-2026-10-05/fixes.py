"""2026-10-05 tam yoxlamanın düzəlişləri: yeni dəlil/zikr sətirləri (bütün cildlərdən).

python3 fixes.py <yedək.json> <çıxış qovluğu>
Çıxış: fixes_seed.sql, fixes_verify.sql. Mərhələ 2–4 ilə eyni üsul: ərəbcə SQL-də ötürülmür,
server `substr(h.text_ar, …)` ilə kəsir; azərbaycanca hədisin (və ya qeydinin) hərfi parçasıdır.
"""
import json, re, sys, os, hashlib

P = os.path.dirname(os.path.abspath(__file__))
SRC, OUT = sys.argv[1], sys.argv[2]
exec(open(f'{P}/../phase2/phase2_ar.py').read().split('# Çıxarış açarı')[0])

d = json.load(open(SRC))
t = d.get('tables', d)
ch = {c['slug']: c for c in t['hadith_chapter']}
bk = {b['slug']: b for b in t['hadith_book']}
H = {}
for r in t['hadith']:
    m = re.match(r'[\s_]*(\d+)\.', r['text_az'] or '')
    c = ch.get(r['chapter_slug'])
    if not (m and c):
        continue
    k = (bk[c['book_slug']]['volume_slug'], int(m[1]))
    if k == ('c1', 1) and not r['chapter_slug'].startswith('c1im'):
        continue
    H.setdefault(k, r)

ORD = {'c1': '1-ci', 'c2': '2-ci', 'c3': '3-cü', 'c4': '4-cü', 'c5': '5-ci', 'c6': '6-cı', 'c7': '7-ci'}

# (mövzu, növ, cild, №, azərbaycanca, ərəbcə lövbərlər, sort_no, oxunuş)
# növ: e — dəlil (az = text_az parçası), d — zikr (az = kitabın qeydindəki tərcümə, oxunuş text_az-dan).
ROWS = [
    # «Dua» addımı: «dörd şey» duası № 361-in Muslim-1262 rəvayətidir (№ 362-nin duası ayrıca qalır).
    ('prayer_dua', 'd', 'c1', 361,
     'Allahımmə! Şübhəsiz ki, mən Cəhənnəm əzabından, qəbr əzabından, həyatın və ölümün fitnəsindən və Məsih Dəccalın fitnəsinin şərindən Sənə sığınıram',
     ('اللهم اني اعوذ بك من عذاب جهنم', 'المسيح الدجال'), -10,
     'Allahummə innii əuuzu bikə min azəəbi Cəhənnəmə, va min azəəbil-qabri, va min fitnətil-məhyəə val-məməəti, va min şərri fitnətil-məsihid-dəccəəli'),
    # İstirahət oturuşu: Amr ibn Səlimə 1-ci və 3-cü rükətdən sonra oturardı.
    ('prayer_rest', 'e', 'c1', 354,
     'o, iki səcdədən başını qaldırdıqda oturduğu halda düz qalardı (bir qədər gözləyərdi), sonra birinci və üçüncü rükətdən qalxardı',
     ('كان اذا رفع راسه من السجدتين', 'والثالثه'), 30, None),
    # İşada qiraət eşidilirdi.
    ('reading_isha', 'e', 'c4', 1589,
     'Rəsulullah sallallahu aleyhi və səlləm dünən axşam Atəmədə (İşa namazında) hansı surəni oxudu?',
     ('باي سوره قرا رسول الله', 'في العتمه'), 0, None),
    ('reading_isha', 'e', 'c4', 1589,
     'Lakin mən bilirəm, O, filan filan surəni oxudu',
     ('ولكني ادري', 'كذا وكذا'), 10, None),
    ('reading_isha', 'e', 'c1', 384,
     'Muaz da Onunla bərabər (İşa namazını) qıldı. Sonra qayıtdı və qövmünə imamlıq edərək (namazda qiraətdə) ilk olaraq Bəqərə surəsini oxumağa başladı',
     ('فصلاها معاذ معه', 'بسوره البقره'), 20, None),
    ('reading_isha', 'e', 'c1', 384,
     'Bu surəni, bu surəni oxu!',
     ('اقرا سوره كذا وسوره كذا',), 30, None),
    ('reading_isha', 'e', 'c1', 578,
     'Əbu Hureyrənin arxasında İşa namazını qıldım, {İzəs-səməəunşəqqat} (İnşiqaq surəsini) oxudu',
     ('صليت خلف ابي هريره العتمه', 'انشقت}'), 40, None),
    # Bayram: dörd nəfər namazı xütbədən əvvəl qılırdı — iddia № 483-dəndir.
    ('eid_first', 'e', 'c1', 483,
     'Rəsulullah sallallahu aleyhi və səlləm, Əbu Bəkr, Ömər və Osman radıyallahu anhum ilə birlikdə bayrama şahid oldum, onların hamısı (bayram) namazı(nı) xütbədən əvvəl qılırdılar',
     ('شهدت العيد مع رسول الله', 'قبل الخطبه'), 10, None),
    # Məğrib + İşa: üç və iki rükəti Nəbinin özü qıldı (Muzdəlifə).
    ('safar_maghrib_isha', 'e', 'c2', 872,
     'O, Məğribi üç rükət və İşanı da iki rükət qıldı',
     ('وصلى المغرب ثلاث ركعات', 'العشاء ركعتين'), -10, None),
    # Ərəfat: Günəş meyl edəndə xütbə, sonra Zöhr və Əsr arada heç nə qılmadan.
    ('safar_arafah', 'e', 'c2', 861,
     'Nəbi sallallahu aleyhi və səlləm Günəş meyl etdikdə Ərəfata gəldi, əmr etdi, (dəvəsi) Qasva Onun üçün gətirildi, vadinin aşağısına gəldi və insanlara xütbə verdi',
     ('اتى عرفات اذا زاغت الشمس', 'فخطب الناس'), 0, None),
    ('safar_arafah', 'e', 'c2', 861,
     'Nəbi sallallahu aleyhi və səlləm Ərəfatda Zöhrü qıldı, sonra Əsri qıldı-yəni onu da Ərəfatda qıldı-və o ikisinin arasında heç bir şey (hər hansı bir sünnət namazı) qılmadı',
     ('صلى الظهر بعرفات', 'ولم يسبح بينهما شيئا'), 10, None),
    # Namazı gecikdirən əmirlər: vaxtında qıl, onlarla qılınan nafilədir.
    ('times_delayed', 'e', 'c1', 249,
     'Diqqət et! (Sən) namazı vaxtında qıl! Sonra onların yanına get;əgər onlar namaz qılmış olsalar, sən artıq namazını yerinə yetirmiş olarsan. Yaxud da onlarla birlikdə namaz qılsan, bu sənin üçün nafilə (namaz) olar',
     ('الا فصل الصلاه لوقتها', 'فكانت لك نافله'), 0, None),
]


def lit(s):
    return "'" + s.replace("'", "''") + "'" if s is not None else 'null'


rows, checks, errors = [], [], []
for topic, kind, vol, no, az, anchors, sort_no, tr in ROWS:
    h = H[(vol, no)]
    try:
        ar = cut(h['text_ar'], *anchors).strip('{}"«» ')
    except KeyError as e:
        errors.append(f'{topic}/{sort_no}: {e}')
        continue
    if len(ar) > 3 * max(len(az), 40):
        errors.append(f'{topic}/{sort_no}: ərəbcə çox uzundur ({len(ar)}): {ar[:80]}…')
    a = h['text_ar'].find(ar)
    assert a >= 0, (topic, 'ərəbcə xam mətnin parçası deyil')
    field = 'note' if kind == 'd' else 'text_az'
    b = (h[field] or '').find(az)
    if b < 0:
        errors.append(f'{topic}/{sort_no}: azərbaycanca {field}-də hərfi tapılmadı')
        continue
    if tr is not None and tr not in h['text_az']:
        errors.append(f'{topic}/{sort_no}: oxunuş text_az-da tapılmadı')
    rows.append(f"({lit(topic)}, {lit('dhikr' if kind == 'd' else 'evidence')}, {h['id']}, {a + 1}, {len(ar)}, {lit(field)}, "
                f"{b + 1}, {len(az)}, {lit(tr)}, {lit('Muheymin ' + ORD[vol] + ' cild, № ' + str(no))}, {sort_no})")
    checks.append((topic, sort_no, hashlib.md5(ar.encode()).hexdigest(), hashlib.md5(az.encode()).hexdigest()))
    print(f'{topic:20} {sort_no:4}  {ar[:70]}')

if errors:
    print('\n'.join(errors)); sys.exit(1)

sql = ("insert into public.salah_evidence (topic, kind, source_type, hadith_id, text_ar, text_az, transliteration, source, sort_no)\n"
       "select v.topic, v.kind, 'hadith', h.id, substr(h.text_ar, v.ar_s, v.ar_n),\n"
       "       case v.az_f when 'note' then substr(h.note, v.az_s, v.az_n) else substr(h.text_az, v.az_s, v.az_n) end,\n"
       "       v.tr, v.src, v.sort_no\n"
       "from (values\n" + ',\n'.join(rows) + "\n) v(topic, kind, hid, ar_s, ar_n, az_f, az_s, az_n, tr, src, sort_no)\n"
       "join public.hadith h on h.id = v.hid\non conflict do nothing;\n")
open(f'{OUT}/fixes_seed.sql', 'w').write(sql)
vals = ','.join(f"('{k}',{s},'{a}','{z}')" for k, s, a, z in checks)
open(f'{OUT}/fixes_verify.sql', 'w').write(
    "select count(*) as expected, count(e.id) as found, count(*) filter (where md5(e.text_ar) = v.ar) as ar_ok, "
    "count(*) filter (where md5(e.text_az) = v.az) as az_ok, string_agg(case when e.id is null or md5(e.text_ar) <> v.ar or md5(e.text_az) <> v.az "
    "then v.topic || '/' || v.s end, ', ') as bad from (values " + vals + ") v(topic, s, ar, az) "
    "left join public.salah_evidence e on e.topic = v.topic and e.sort_no = v.s")
