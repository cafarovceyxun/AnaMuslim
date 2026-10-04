"""Mərhələ 4 (Nafilə və cənazə) üçün tək mənbə: SalahTopic enum sətirləri + salah_evidence seed SQL.

python3 phase4_seed.py <db.json> <çıxış qovluğu>
Çıxış: salah4_seed.sql, salah4_verify.sql, salah4_topics.kt.txt. Seed `on conflict do nothing` ilədir.
Mərhələ 2-nin `phase2_seed.py`-si ilə eyni üsul: SQL ərəbcəni ötürmür, `substr(h.text_ar, …)` ilə server kəsir.
"""
import json, re, sys, os, hashlib

P = os.path.dirname(os.path.abspath(__file__))
DB, OUT = sys.argv[1], sys.argv[2]
exec(open(f'{P}/data.py').read())
exec(open(f'{P}/phase4_ar.py').read().replace('__file__', repr(f'{P}/phase4_ar.py')))

t = json.load(open(DB))
H = {}
for r in t['hadith']:
    m = re.match(r'[\s_]*(\d+)\.', r['text_az'] or '')
    if not m:
        continue
    k = (r['chapter_slug'][:2], int(m[1]))
    if k == ('c1', 1) and not r['chapter_slug'].startswith('c1im'):
        continue
    H.setdefault(k, r)

ORD = {'c1': '1-ci', 'c2': '2-ci', 'c3': '3-cü', 'c4': '4-cü', 'c5': '5-ci', 'c6': '6-cı', 'c7': '7-ci'}

# (açar, admin seçim adı, [(çıxarış/zikr açarı, növ)]) — növ: e = dəlil, w = qadınlar üçün, d = zikr (ZK-dan).
TOPICS = [
    # Sünnət namazları
    ('sunnah_12', 'Sünnət: on iki rükətin fəziləti', [('r_12', 'e'), ('r_12_kept', 'e')]),
    ('sunnah_ten', 'Sünnət: İbn Ömərin on rükəti', [('r_ten', 'e'), ('r_list', 'e'), ('r_jumuah', 'e')]),
    ('sunnah_between', 'Sünnət: hər əzan ilə iqamə arası', [('between', 'e')]),
    ('fajr_never', 'Fəcrin iki rükəti: heç tərk etmədi', [('f_never', 'e')]),
    ('fajr_eager', 'Fəcrin iki rükəti: tələsərdi', [('f_eager', 'e')]),
    ('fajr_light', 'Fəcrin iki rükəti: xəfif', [('f_light', 'e'), ('f_fatiha', 'e')]),
    ('fajr_reading', 'Fəcrin iki rükəti: nə oxunur', [('f_ayah1', 'e'), ('f_ayah2', 'e')]),
    ('fajr_lie', 'Fəcrin iki rükəti: sonra uzanmaq', [('f_right', 'e'), ('f_lie', 'e')]),
    ('maghrib_before', 'Sünnət: Məğribdən əvvəl iki rükət', [('maghrib_rush', 'e'), ('maghrib_many', 'e'), ('maghrib_busy', 'e')]),
    ('sunnah_nahy', 'Sünnət: Sübh və Əsrdən sonra qadağa', [('nahy', 'e')]),
    ('sunnah_asr_two', 'Sünnət: Əsrdən sonrakı iki rükət', [('asr_two', 'e'), ('asr_masjid', 'e')]),
    # Duha və nafilələr
    ('duha_will', 'Duha: vəsiyyət', [('duha_will', 'e')]),
    ('duha_eight', 'Duha: fəth günü səkkiz rükət', [('duha_eight', 'e'), ('duha_time', 'e')]),
    ('duha_full', 'Duha: xəfif, amma tam', [('duha_full', 'e')]),
    ('duha_anas', 'Duha: Ənəsin sözü', [('duha_anas', 'e')]),
    ('duha_aisha', 'Duha: Aişənin sözü', [('duha_aisha', 'e'), ('duha_aisha_pray', 'e')]),
    ('duha_ibn_umar', 'Duha: İbn Ömərin sözü', [('duha_umar', 'e')]),
    ('nafl_return', 'Nafilə: səfərdən qayıdanda', [('return_two', 'e')]),
    ('nafl_bilal', 'Nafilə: dəstəmazdan sonra', [('bilal', 'e')]),
    ('nafl_home', 'Nafilə: evdə qılmaq', [('home', 'e')]),
    ('nafl_able', 'Nafilə: az da olsa davamlı', [('able', 'e'), ('lasting', 'e')]),
    ('nafl_tired', 'Nafilə: yorulanda oturmaq', [('zaynab', 'e')]),
    ('nafl_women', 'Nafilə: qadınlar üçün', [('hush', 'w')]),
    # Gecə namazı və vitr
    ('night_dawud', 'Gecə: Davudun namazı', [('dawud', 'e')]),
    ('night_knots', 'Gecə: şeytanın üç düyünü', [('knots_say', 'e'), ('knots', 'e')]),
    ('night_slept', 'Gecə: səhərədək yatan', [('slept', 'e')]),
    ('night_rights', 'Gecə: özünü yormamaq', [('rights', 'e')]),
    ('night_time', 'Gecə: nə vaxt qalxardı', [('first_last', 'e'), ('rooster', 'e')]),
    ('witr_any', 'Vitr: gecənin hər vaxtında', [('witr_any', 'e')]),
    ('night_sleep_after', 'Gecə: vitrdən sonra yatmaq', [('sleep_after', 'e')]),
    ('night_two_two', 'Gecə: iki-iki', [('two_two', 'e')]),
    ('witr_one', 'Vitr: bir rükət, sonuncu', [('witr_one', 'e'), ('witr_last', 'e')]),
    ('night_form_a', 'Gecə: 4 + 4 + 3', [('eleven', 'e'), ('four_four', 'e')]),
    ('night_form_b', 'Gecə: hər iki rükətdə salam', [('salam_each', 'e')]),
    ('night_form_c', 'Gecə: Fəcrin sünnəti ilə on üç', [('thirteen', 'e')]),
    ('night_form_d', 'Gecə: Zeydin izlədiyi on üç', [('zayd', 'e')]),
    ('night_form_e', 'Gecə: İbn Abbasın gecələri', [('six_pairs', 'e'), ('five', 'e')]),
    ('night_eyes', 'Gecə: gözlərim yatır, qəlbim yox', [('eyes', 'e')]),
    ('night_wake_face', 'Gecə: qalxanda üzü silmək', [('wipe', 'e')]),
    ('night_wake_quran', 'Gecə: Ali-İmranın sonu', [('ali_imran', 'e'), ('sky', 'e')]),
    ('night_wake_miswak', 'Gecə: misvak və dəstəmaz', [('miswak', 'e')]),
    ('night_dua_tahajjud', 'Gecə duası: təhəccüd', [('tahajjud', 'd'), ('tahajjud_alt', 'd')]),
    ('night_dua_nur', 'Gecə duası: nur', [('nur', 'd')]),
    ('night_dua_sajda', 'Gecə duası: səcdədə', [('sajda', 'd')]),
    ('night_right_side', 'Gecə: tək qoşulan sağda', [('right_side', 'e')]),
    ('night_ramadan', 'Gecə: Ramazanda camaatla', [('ramadan_fear', 'e')]),
    ('night_tartil', 'Gecə: tərtil', [('tartil', 'e')]),
    ('night_sitting', 'Gecə: oturaraq qılmaq', [('sitting', 'e')]),
    ('night_long_sajda', 'Gecə: uzun səcdə', [('sajda_fifty', 'e')]),
    ('night_women', 'Gecə: qadınlar üçün', [('wake_witr', 'w')]),
    # Səhv səcdəsi
    ('sahw_shaytan', 'Səhv: şeytanın vəsvəsəsi', [('shaytan', 'e')]),
    ('sahw_sit', 'Səhv: oturaraq iki səcdə', [('sit_two', 'e')]),
    ('sahw_yaqin', 'Səhv: yəqin üzərində, salamdan əvvəl', [('yaqin', 'e'), ('yaqin_why', 'e')]),
    ('sahw_taharri', 'Səhv: doğrunu araşdırmaq, salamdan sonra', [('taharri', 'e'), ('human', 'e')]),
    ('sahw_extra', 'Səhv: beş rükət qılmaq', [('extra_after', 'e'), ('extra_then_salam', 'e')]),
    ('sahw_less', 'Səhv: tez salam vermək', [('dhul_q', 'e'), ('dhul', 'e'), ('dhul_talk', 'e')]),
    ('sahw_tashahhud', 'Səhv: birinci təşəhhüdü unutmaq', [('tash_forgot', 'e'), ('tash_fix', 'e')]),
    # Tilavət səcdəsi
    ('tilawah_out', 'Tilavət: namazdan xaric', [('t_out', 'e'), ('t_crowd', 'e')]),
    ('tilawah_najm', 'Tilavət: Nəcm', [('t_najm', 'e')]),
    ('tilawah_inshiqaq', 'Tilavət: İnşiqaq', [('t_isha', 'e')]),
    ('tilawah_alaq', 'Tilavət: Aləq', [('t_late', 'e')]),
    # Cənazə
    ('janazah_qirat', 'Cənazə: qirat', [('qirat', 'e'), ('qirat_mount', 'e'), ('qirat_lost', 'e')]),
    ('janazah_wash', 'Cənazə: yumaq', [('wash', 'e'), ('wash_odd', 'e')]),
    ('janazah_wash_right', 'Cənazə: sağdan başlamaq', [('wash_start', 'e')]),
    ('janazah_shroud', 'Cənazə: kəfən', [('shroud', 'e')]),
    ('janazah_hair', 'Cənazə: qadının saçı', [('hair', 'w')]),
    ('janazah_rows', 'Cənazə: musallə və səflər', [('four', 'e'), ('row23', 'e')]),
    ('janazah_takbir', 'Cənazə: dörd təkbir', [('four_short', 'e')]),
    ('janazah_fatiha', 'Cənazə: Fatihə', [('fatiha', 'e'), ('fatiha_why', 'e')]),
    ('janazah_istighfar', 'Cənazə: bağışlanma diləmək', [('istighfar', 'e')]),
    ('janazah_grave', 'Cənazə: qəbrə namaz', [('grave', 'e'), ('grave_take', 'e'), ('grave_light', 'e')]),
    ('janazah_abroad', 'Cənazə: uzaqda ölənə', [('abroad', 'e')]),
    ('janazah_martyrs', 'Cənazə: şəhidlərə', [('uhud', 'e')]),
    ('janazah_debt', 'Cənazə: borcluya', [('debt', 'e'), ('debt_pray', 'e'), ('abu_qatada', 'e'), ('debt_mine', 'e')]),
    ('janazah_munafiq', 'Cənazə: münafiqə', [('munafiq', 'e')]),
    ('janazah_graves', 'Cənazə: qəbirlərin üstündə', [('graves', 'e')]),
    ('janazah_place', 'Cənazə: harada qoyulur', [('place', 'e')]),
    ('janazah_women', 'Cənazə: qadınlar üçün', [('aisha_woman', 'w')]),
]

# Zikrlərin oxunuşu və tərcüməsi (ərəbcə ZK_AR-dan): oxunuş text_az-dakı {…} blokundan, tərcümə qeyddən.
BRACE = re.compile(r'\{([^{}]*)\}')
def fold_lat(s):
    return re.sub(r'[^a-zəığüöşçA-ZƏIĞÜÖŞÇ]', '', s).lower()
def brace(text, anchor):
    for m in BRACE.finditer(text):
        if fold_lat(anchor) in fold_lat(m[1]):
            return re.sub(r'\s+', ' ', m[1]).strip()
    raise KeyError(f'oxunuş tapılmadı: {anchor}')
def between(note, a, b):
    i, j = note.find(a), note.find(b)
    if i < 0 or j < i:
        raise KeyError(f'tərcümə tapılmadı: {a}')
    return note[i:j + len(b)]
ZKD = {}
for k, z in ZK.items():
    h = H[z['ref']]
    tr = brace(h['text_az'], z['trAnchor'])
    mean = between(h['note'] or '', z['meanFrom'], z['meanTo']).strip('"“” ')
    if z.get('foot'):
        mean = re.sub(r'\s*\(\d\)', '', mean)
    ZKD[k] = {'ref': z['ref'], 'tr': tr, 'mean': mean}
    if z.get('alt'):
        a = z['alt']
        ZKD[k + '_alt'] = {'ref': z['ref'], 'tr': brace(h['text_az'], a['trAnchor']), 'mean': a['mean']}

KIND = {'e': 'evidence', 'w': 'women', 'd': 'dhikr'}


def lit(s):
    return "'" + s.replace("'", "''") + "'" if s is not None else 'null'


keys = [k for k, _, _ in TOPICS]
assert len(keys) == len(set(keys)), 'təkrar mövzu açarı'
assert all(re.fullmatch(r'[a-z][a-z0-9_]{0,39}', k) for k in keys)

rows, seen, checks, errors = [], set(), [], []
for key, _, items in TOPICS:
    for i, (src, kind) in enumerate(items):
        try:
            if kind == 'd':
                z = ZKD[src]; vol, no = z['ref']; h = H[(vol, no)]
                ar = cut(h['text_ar'], *ZK_AR[src]); az = z['mean']; tr = z['tr']; az_field = 'note'
            else:
                vol, no, az = EX[src]; h = H[(vol, no)]
                ar = cut(h['text_ar'], *AR[src]); tr = None; az_field = 'text_az'
        except KeyError as e:
            errors.append(f'{src}: {e}'); continue
        ar = ar.strip('{}"«» ')
        if len(ar) > 3 * max(len(az), 40) or len(ar) > 900:
            errors.append(f'{src}: ərəbcə dilim çox uzundur ({len(ar)}): {ar[:80]}…')
        a = h['text_ar'].find(ar)
        assert a >= 0, (src, 'ərəbcə xam mətnin parçası deyil')
        u = (key, ar, h['id'])
        assert u not in seen, ('unikal açar təkrarlanır', key, src)
        seen.add(u)
        b = (h[az_field] or '').find(az)
        if b < 0 and kind == 'd':
            az_field = 'text_az'; b = h['text_az'].find(az)
        if kind != 'd':
            assert re.sub(r'\s+', ' ', az) in re.sub(r'\s+', ' ', h['text_az']), (src, 'azərbaycanca hərfi deyil')
        rows.append(f"({lit(key)}, {lit(KIND[kind])}, {h['id']}, {a + 1}, {len(ar)}, {lit(az_field) if b >= 0 else 'null'}, "
                    f"{b + 1 if b >= 0 else 'null'}, {len(az) if b >= 0 else 'null'}, {lit(az) if b < 0 else 'null'}, {lit(tr)}, "
                    f"{lit('Muheymin ' + ORD[vol] + ' cild, № ' + str(no))}, {i * 10})")
        checks.append((key, i * 10, hashlib.md5(ar.encode()).hexdigest(), hashlib.md5(az.encode()).hexdigest()))

if errors:
    print('\n'.join(errors)); sys.exit(1)

sql = ("insert into public.salah_evidence (topic, kind, source_type, hadith_id, text_ar, text_az, transliteration, source, sort_no)\n"
       "select v.topic, v.kind, 'hadith', h.id, substr(h.text_ar, v.ar_s, v.ar_n),\n"
       "       case v.az_f when 'text_az' then substr(h.text_az, v.az_s, v.az_n) when 'note' then substr(h.note, v.az_s, v.az_n) else v.az_lit end,\n"
       "       v.tr, v.src, v.sort_no\n"
       "from (values\n" + ',\n'.join(rows) + "\n) v(topic, kind, hid, ar_s, ar_n, az_f, az_s, az_n, az_lit, tr, src, sort_no)\n"
       "join public.hadith h on h.id = v.hid\non conflict do nothing;\n")
open(f'{OUT}/salah4_seed.sql', 'w').write(sql)
vals = ','.join(f"('{k}',{s},'{a}','{z}')" for k, s, a, z in checks)
open(f'{OUT}/salah4_verify.sql', 'w').write(
    "select count(*) as expected, count(e.id) as found, count(*) filter (where md5(e.text_ar) = v.ar) as ar_ok, "
    "count(*) filter (where md5(e.text_az) = v.az) as az_ok, string_agg(case when e.id is null or md5(e.text_ar) <> v.ar or md5(e.text_az) <> v.az "
    "then v.topic || '/' || v.s end, ', ') as bad from (values " + vals + ") v(topic, s, ar, az) "
    "left join public.salah_evidence e on e.topic = v.topic and e.sort_no = v.s")
kt = '\n'.join(f'    {k.upper()}("{k}", "{title}"),' for k, title, _ in TOPICS)
open(f'{OUT}/salah4_topics.kt.txt', 'w').write(kt)
print(len(TOPICS), 'mövzu', len(rows), 'sətir', len(sql), 'bayt')
