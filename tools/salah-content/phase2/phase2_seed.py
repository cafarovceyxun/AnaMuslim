"""Mərhələ 2 (Əzan və namaz) üçün tək mənbə: SalahTopic enum sətirləri + salah_evidence seed SQL.

python3 phase2_seed.py <db.json> <çıxış qovluğu>
Çıxış: salah2_seed.sql, salah2_topics.kt.txt. Seed `on conflict do nothing` ilədir — təkrar işlətmək zərərsizdir.
"""
import json, re, sys, os, hashlib

P = os.path.dirname(os.path.abspath(__file__))
DB, OUT = sys.argv[1], sys.argv[2]
exec(open(f'{P}/data.py').read())
exec(open(f'{P}/phase2_ar.py').read())

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
    # Namazın qılınışı — addımlar
    ('prayer_teach', 'Namaz: № 205 təlimi', [('redo', 'e'), ('teach', 'e')]),
    ('prayer_niyyah', 'Namaz: niyyət', [('niyyah', 'e')]),
    ('prayer_takbir', 'Namaz: təkbir', [('takbir', 'd'), ('takbir', 'e'), ('hands_up', 'e')]),
    ('prayer_hands', 'Namaz: əllər sinədə', [('right_left', 'e')]),
    ('prayer_opening', 'Namaz: açılış duası', [('istiftah', 'd'), ('silence', 'e')]),
    ('prayer_reading', 'Namaz: qiraət', [('fatiha', 'e'), ('first_two', 'e'), ('last_two', 'e'), ('opening', 'e')]),
    ('prayer_ruku_takbir', 'Namaz: rükuya təkbir', [('takbir', 'd'), ('hands_ruku', 'e')]),
    ('prayer_ruku', 'Namaz: rüku', [('ruku1', 'd'), ('ruku2', 'd'), ('ruku_knees', 'e'), ('ruku_calm', 'e'), ('ruku_interlock', 'e')]),
    ('prayer_rise', 'Namaz: rükudan qalxmaq', [('rise', 'd'), ('hands_rise', 'e')]),
    ('prayer_standing', 'Namaz: rükudan sonra qiyam', [('right_left', 'e'), ('k_hips', 'e'), ('k_hips_def', 'e'), ('rise_calm', 'e'), ('rise_long', 'e')]),
    ('prayer_sajda', 'Namaz: səcdə', [('ruku1', 'd'), ('ruku2', 'd'), ('seven', 'e'), ('seven_list', 'e'), ('nose', 'e'),
                                        ('elbows_up', 'e'), ('not_dog', 'e'), ('toes_qibla', 'e'), ('hands_not', 'e')]),
    ('prayer_between', 'Namaz: iki səcdə arası', [('takbir_each', 'e'), ('sit_left', 'e'), ('sit_mid', 'e'), ('between_long', 'e')]),
    ('prayer_sajda2', 'Namaz: ikinci səcdə', [('takbir_each', 'e')]),
    ('prayer_rest', 'Namaz: ikinci rükətə qalxmaq', [('rest_sit', 'e'), ('all_prayer', 'e')]),
    ('prayer_tashahhud', 'Namaz: təşəhhüd', [('tashahhud', 'd'), ('sit_mid', 'e'), ('sit_last', 'e'), ('finger', 'e'),
                                               ('finger_qibla', 'e'), ('gaze', 'e'), ('after_death', 'e')]),
    ('prayer_dua', 'Namaz: təşəhhüddən sonra dua', [('refuge', 'd'), ('four_refuge', 'e'), ('choose_dua', 'e')]),
    ('prayer_salam', 'Namaz: salam', [('salam_turn', 'e')]),
    # Rükət sayı və qiraət
    ('rakat_fajr', 'Rükət: Sübh', [('r_fajr', 'e')]),
    ('rakat_zuhr', 'Rükət: Zöhr', [('r_zuhr', 'e')]),
    ('rakat_four', 'Rükət: muqimə dörd', [('r_four', 'e')]),
    ('rakat_maghrib', 'Rükət: Məğrib', [('r_maghrib', 'e')]),
    ('reading_silent', 'Qiraət: Zöhr və Əsr səssiz', [('silent', 'e')]),
    ('reading_fajr', 'Qiraət: Fəcrdə eşidilirdi', [('fajr_heard', 'e')]),
    ('reading_maghrib', 'Qiraət: Məğribdə eşidilirdi', [('maghrib_heard', 'e')]),
    # Vaxtlar
    ('times_between', 'Vaxtlar: Cibrilin iki günü', [('t_between', 'e')]),
    ('times_ontime', 'Vaxtlar: vaxtında namaz', [('t_ontime', 'e')]),
    ('times_forgot', 'Vaxtlar: unudulan namaz', [('t_forgot', 'e')]),
    ('times_rakah', 'Vaxtlar: bir rükətə çatan', [('t_rakah', 'e')]),
    ('times_cool', 'Vaxtlar: istidə Zöhr', [('t_cool', 'e')]),
    ('times_isha', 'Vaxtlar: İşanı gecikdirmək', [('t_isha', 'e')]),
    ('times_after', 'Vaxtlar: Sübhdən və Əsrdən sonra', [('t_after', 'e')]),
    ('times_sun', 'Vaxtlar: Günəş doğarkən və batarkən', [('t_sun', 'e')]),
    ('times_noon', 'Vaxtlar: Günəş ortada', [('t_noon', 'e')]),
    ('times_women', 'Vaxtlar: qadınlar Sübhdə', [('t_women_fajr', 'w')]),
    # Qiblə
    ('qibla_ayah', 'Qiblə: Bəqərə 144', [('q_ayah', 'e')]),
    ('qibla_turned', 'Qiblə: namazda dönmək', [('q_turned', 'e')]),
    ('qibla_fard', 'Qiblə: fərz qibləyə', [('q_fard', 'e')]),
    ('qibla_mount', 'Qiblə: minik üstündə nafilə', [('q_mount', 'e')]),
    # Əzan
    ('adhan_pairs', 'Əzan: cüt, iqamə tək', [('a_pairs', 'e')]),
    ('adhan_repeat', 'Əzan: müəzzini təkrarlamaq', [('a_repeat', 'e')]),
    ('adhan_bilal', 'Əzan: ilk müəzzin', [('a_bilal', 'e')]),
    ('adhan_loud', 'Əzan: uca səs', [('a_loud', 'e')]),
    ('adhan_fajr', 'Əzan: Fəcrin iki əzanı', [('a_two_fajr', 'e')]),
    ('adhan_rain', 'Əzan: soyuq və yağışda', [('a_rain', 'e')]),
    ('adhan_leave', 'Əzan: sonra çıxıb getmək', [('a_leave', 'e')]),
    # Namazdan sonra
    ('after_takbir', 'Namazdan sonra: uca təkbir', [('after_takbir', 'e')]),
    ('after_salam', 'Namazdan sonra: Allahummə əntəs-salam', [('salam_after', 'd')]),
    ('after_33', 'Namazdan sonra: 33 + 33 + 33', [('after_33', 'e')]),
    ('after_tahlil', 'Namazdan sonra: təhlil', [('tahlil', 'd')]),
    ('after_left', 'Namazdan sonra: hansı tərəfdən getmək', [('after_left', 'e')]),
    # Namazda bilmək lazımdır
    ('know_light', 'Namazda: yüngül, amma tam', [('k_light', 'e')]),
    ('know_sky', 'Namazda: göyə baxmaq', [('k_sky', 'e')]),
    ('know_hips', 'Namazda: ixtisar', [('k_hips', 'e'), ('k_hips_def', 'e')]),
    ('know_look', 'Namazda: boylanmaq', [('k_look', 'e')]),
    ('know_talk', 'Namazda: danışmaq', [('k_talk', 'e')]),
    ('know_spit', 'Namazda: tüpürmək', [('k_spit', 'e')]),
    ('know_food', 'Namazda: yemək hazır ikən', [('k_food', 'e')]),
    ('know_shoulders', 'Namazda: çiyinlər', [('k_shoulders', 'e')]),
    ('know_sleepy', 'Namazda: mürgü', [('k_sleepy', 'e')]),
    ('know_child', 'Namazda: uşaq qucaqda', [('k_child', 'e')]),
    ('know_pebbles', 'Namazda: daşları düzəltmək', [('k_pebbles', 'e')]),
    ('know_tasbih', 'Namazda: bir şey üz verəndə', [('k_tasbih', 'e')]),
    ('know_enter', 'Namazda: məscidə girəndə', [('k_enter', 'e')]),
    ('know_sutra', 'Sütrə: nə boyda', [('k_sutra', 'e')]),
    ('know_pass_sin', 'Sütrə: qarşıdan keçmək', [('k_pass_sin', 'e')]),
    ('know_pass', 'Sütrə: keçmək istəyəni', [('k_pass', 'e')]),
    ('know_clap', 'Namazda: qadınlar əl çırpır', [('k_clap', 'w')]),
    ('know_mosque', 'Namazda: qadınlar məsciddə', [('k_mosque_w', 'w')]),
]
KIND = {'e': 'evidence', 'w': 'women', 'd': 'dhikr'}


def lit(s):
    return "'" + s.replace("'", "''") + "'" if s is not None else 'null'


keys = [k for k, _, _ in TOPICS]
assert len(keys) == len(set(keys)), 'təkrar mövzu açarı'
assert all(re.fullmatch(r'[a-z][a-z0-9_]{0,39}', k) for k in keys)

rows, seen, checks = [], set(), []
for key, _, items in TOPICS:
    for i, (src, kind) in enumerate(items):
        if kind == 'd':
            z = ZK[src]; vol, no = z['ref']; h = H[(vol, no)]
            ar = cut(h['text_ar'], *ZK_AR[src]); az = z['mean']; tr = z['tr']; az_field = 'note'
        else:
            vol, no, az = EX[src]; h = H[(vol, no)]
            ar = cut(h['text_ar'], *AR[src]); tr = None; az_field = 'text_az'
        a = h['text_ar'].find(ar)
        assert a >= 0, (src, 'ərəbcə xam mətnin parçası deyil')
        u = (key, ar, h['id'])
        assert u not in seen, ('unikal açar təkrarlanır', key, src)
        seen.add(u)
        # Ərəbcə server tərəfdə kəsilir: substr(h.text_ar, start, len) — Python indeksləri kod nöqtəsidir, Postgres da.
        ar_sql = f"substr(h.text_ar, {a + 1}, {len(ar)})"
        b = (h[az_field] or '').find(az)
        az_sql = f"substr(h.{az_field}, {b + 1}, {len(az)})" if b >= 0 else lit(az)
        if kind != 'd':
            assert re.sub(r'\s+', ' ', az) in re.sub(r'\s+', ' ', h['text_az']), (src, 'azərbaycanca hərfi deyil')
        rows.append(f"({lit(key)}, {lit(KIND[kind])}, {h['id']}, {a + 1}, {len(ar)}, {lit(az_field) if b >= 0 else 'null'}, "
                    f"{b + 1 if b >= 0 else 'null'}, {len(az) if b >= 0 else 'null'}, {lit(az) if b < 0 else 'null'}, {lit(tr)}, "
                    f"{lit('Muheymin ' + ORD[vol] + ' cild, № ' + str(no))}, {i * 10})")
        checks.append((key, i * 10, hashlib.md5(ar.encode()).hexdigest(), hashlib.md5(az.encode()).hexdigest()))

sql = ("insert into public.salah_evidence (topic, kind, source_type, hadith_id, text_ar, text_az, transliteration, source, sort_no)\n"
       "select v.topic, v.kind, 'hadith', h.id, substr(h.text_ar, v.ar_s, v.ar_n),\n"
       "       case v.az_f when 'text_az' then substr(h.text_az, v.az_s, v.az_n) when 'note' then substr(h.note, v.az_s, v.az_n) else v.az_lit end,\n"
       "       v.tr, v.src, v.sort_no\n"
       "from (values\n" + ',\n'.join(rows) + "\n) v(topic, kind, hid, ar_s, ar_n, az_f, az_s, az_n, az_lit, tr, src, sort_no)\n"
       "join public.hadith h on h.id = v.hid\non conflict do nothing;\n")
open(f'{OUT}/salah2_seed.sql', 'w').write(sql)
vals = ','.join(f"('{k}',{s},'{a}','{z}')" for k, s, a, z in checks)
open(f'{OUT}/salah2_verify.sql', 'w').write(
    "select count(*) as expected, count(e.id) as found, count(*) filter (where md5(e.text_ar) = v.ar) as ar_ok, "
    "count(*) filter (where md5(e.text_az) = v.az) as az_ok, string_agg(case when e.id is null or md5(e.text_ar) <> v.ar or md5(e.text_az) <> v.az "
    "then v.topic || '/' || v.s end, ', ') as bad from (values " + vals + ") v(topic, s, ar, az) "
    "left join public.salah_evidence e on e.topic = v.topic and e.sort_no = v.s")
kt = '\n'.join(f'    {k.upper()}("{k}", "{title}"),' for k, title, _ in TOPICS)
open(f'{OUT}/salah2_topics.kt.txt', 'w').write(kt)
print(len(TOPICS), 'mövzu', len(rows), 'sətir', len(sql), 'bayt')
