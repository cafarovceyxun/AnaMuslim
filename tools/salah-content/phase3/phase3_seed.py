"""Mərhələ 3 (Camaat və xüsusi namazlar) üçün tək mənbə: SalahTopic enum sətirləri + salah_evidence seed SQL.

python3 phase3_seed.py <db.json> <çıxış qovluğu>
Çıxış: salah3_seed.sql, salah3_verify.sql, salah3_topics.kt.txt. Seed `on conflict do nothing` ilədir.
Mərhələ 2-nin `phase2_seed.py`-si ilə eyni üsul: SQL ərəbcəni ötürmür, `substr(h.text_ar, …)` ilə server kəsir.
"""
import json, re, sys, os, hashlib

P = os.path.dirname(os.path.abspath(__file__))
DB, OUT = sys.argv[1], sys.argv[2]
exec(open(f'{P}/data.py').read())
exec(open(f'{P}/phase3_ar.py').read().replace('__file__', repr(f'{P}/phase3_ar.py')))

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
    # Camaat namazı
    ('jamaah_25', 'Camaat: iyirmi beş dərəcə', [('c_25', 'e')]),
    ('jamaah_steps', 'Camaat: məscidə addımlar', [('c_steps', 'e'), ('c_wait', 'e')]),
    ('jamaah_isha_fajr', 'Camaat: İşa və Sübh', [('c_isha_fajr', 'e'), ('c_angels', 'e')]),
    ('jamaah_first_row', 'Camaat: əzan və ilk səf', [('c_first_row', 'e')]),
    ('jamaah_absent', 'Camaat: geri qalmaq', [('c_nifaq', 'e'), ('c_carried', 'e')]),
    ('imam_quran', 'İmam: Quranı ən çox bilən', [('imam_quran', 'e'), ('imam_salim', 'e'), ('imam_boy', 'e')]),
    ('imam_oldest', 'İmam: yaşıdlarda ən böyüyü', [('imam_oldest', 'e')]),
    ('imam_like_me', 'İmam: Nəbi kimi qılmaq', [('imam_like_me', 'e')]),
    ('follow_imam', 'İmama tabe: ona uyulsun deyə', [('follow', 'e')]),
    ('follow_amin', 'İmama tabe: Amin', [('amin', 'e'), ('amin_angels', 'e')]),
    ('follow_rabbana', 'İmama tabe: Rabbənə ləkəl-həmd', [('rabbana', 'd'), ('follow_rabbana', 'e')]),
    ('follow_before', 'İmama tabe: imamdan qabaq', [('donkey', 'e')]),
    ('follow_after', 'İmama tabe: imamdan sonra səcdə', [('follow_after', 'e')]),
    ('follow_sitting', 'İmama tabe: oturaraq qılan imam', [('follow_sitting', 'e'), ('follow_last', 'e')]),
    ('follow_complete', 'İmama tabe: rüku və səcdəni tamamlamaq', [('ruku_complete', 'e')]),
    ('late_walk', 'Gec gələn: tələsmədən', [('walk', 'e')]),
    ('late_stand', 'Gec gələn: imamı görməmiş durmamaq', [('dont_stand', 'e')]),
    ('late_iqama', 'Gec gələn: iqamədən sonra başqa namaz', [('iqama_four', 'e'), ('iqama_only', 'e')]),
    ('late_hamd', 'Gec gələn: səfə qoşulanın həmdi', [('hamd', 'd'), ('hamd_late', 'e')]),
    ('light_weak', 'İmam yüngül qıldırsın', [('light', 'e'), ('light_alone', 'e')]),
    ('light_angry', 'İmam: insanları uzaqlaşdırmaq', [('light_angry', 'e')]),
    ('light_muadh', 'İmam: Muaza tövsiyə', [('light_muadh', 'e')]),
    ('light_short', 'İmam: xəfif, amma tam', [('light_short', 'e')]),
    ('qunut_after', 'Qunut: rükudan sonra', [('qunut_after', 'e')]),
    ('qunut_when', 'Qunut: kimin üçün', [('qunut_when', 'e')]),
    ('qunut_month', 'Qunut: bir ay, sonra tərk', [('qunut_left', 'e'), ('qunut_month', 'e')]),
    ('qunut_ayah', 'Qunut: Ali-İmran 128', [('qunut_ayah', 'e')]),
    ('qunut_abuhurayra', 'Qunut: Əbu Hureyrə', [('qunut_abuhurayra', 'e')]),
    # Səflər
    ('rows_or', 'Səflər: ya düzəldərsiniz', [('row_or', 'e')]),
    ('rows_straight', 'Səflər: ox kimi düz', [('row_arrows', 'e'), ('row_then_takbir', 'e')]),
    ('rows_tight', 'Səflər: sıxlaşmaq', [('row_tight', 'e'), ('row_shoulder', 'e')]),
    ('rows_gaps', 'Səflər: boşluq qoymamaq', [('row_gaps', 'e')]),
    ('rows_sees', 'Səflər: arxadan görürəm', [('row_sees', 'e')]),
    ('seat_one', 'Səflər: bir kişi sağda', [('pos_one', 'e')]),
    ('seat_one_woman', 'Səflər: kişi və qadın', [('pos_one_woman', 'e')]),
    ('seat_two_women', 'Səflər: kişi və iki qadın', [('pos_two_women', 'e')]),
    ('seat_two_boys', 'Səflər: iki uşaq və qadın', [('pos_two_boys', 'e')]),
    ('rows_women', 'Səflər: qadınlar arxada', [('pos_women_behind', 'w'), ('women_head', 'w')]),
    # Cümə
    ('jumuah_day', 'Cümə: bu günə hidayət', [('j_day', 'e')]),
    ('jumuah_ghusl', 'Cümə: qüsl', [('j_ghusl', 'e'), ('j_ghusl_wajib', 'e'), ('j_ghusl_week', 'e')]),
    ('jumuah_prep', 'Cümə: təmizlik, yağ, ətir', [('j_prep', 'e')]),
    ('jumuah_early', 'Cümə: erkən getmək', [('j_early', 'e'), ('j_angels_listen', 'e'), ('j_pages', 'e')]),
    ('jumuah_no_split', 'Cümə: iki nəfərin arasını ayırmamaq', [('j_no_split', 'e')]),
    ('jumuah_pray', 'Cümə: gəlib namaz qılmaq', [('j_pray', 'e'), ('j_reward', 'e')]),
    ('jumuah_quiet', 'Cümə: xütbədə susmaq', [('j_quiet', 'e')]),
    ('jumuah_adhan', 'Cümə: əzan', [('j_adhan', 'e'), ('j_one_muezzin', 'e')]),
    ('jumuah_khutbah', 'Cümə: iki xütbə', [('j_standing', 'e'), ('j_two_khutbah', 'e'), ('j_finger', 'e')]),
    ('jumuah_rakah', 'Cümə: iki rükət və surələr', [('j_two_rakah', 'e'), ('j_surahs', 'e'), ('j_ghashiya', 'e')]),
    ('jumuah_home', 'Cümə: sonra evdə iki rükət', [('j_home_two', 'e'), ('j_not_four', 'e')]),
    ('jumuah_time', 'Cümə: vaxtı', [('j_time', 'e'), ('j_qailah', 'e')]),
    ('jumuah_hour', 'Cümə: duanın qəbul olduğu saat', [('j_hour', 'e'), ('j_hour_short', 'e')]),
    ('jumuah_ghusl_why', 'Cümə: qüslün səbəbi', [('j_ghusl_why', 'e'), ('j_ghusl_umar', 'e')]),
    ('jumuah_tahiyya', 'Cümə: xütbədə gələn', [('j_tahiyya_ask', 'e'), ('j_tahiyya', 'e')]),
    ('jumuah_fajr', 'Cümə: Sübhdə qiraət', [('j_fajr', 'e')]),
    ('jumuah_caravan', 'Cümə: xütbəni qoyub getmək', [('j_caravan', 'e')]),
    # Səfər
    ('safar_sadaqa', 'Səfər: qəsr sədəqədir', [('s_sadaqa', 'e')]),
    ('safar_two', 'Səfər: namaz iki rükət qaldı', [('s_two', 'e')]),
    ('safar_town', 'Səfər: şəhərdən çıxanda', [('s_town', 'e')]),
    ('safar_safe', 'Səfər: əmin-amanlıqda da', [('s_safe', 'e')]),
    ('safar_zuhr_asr', 'Səfər: Zöhr və Əsr', [('s_zuhr_asr', 'e'), ('s_zuhr_first', 'e')]),
    ('safar_maghrib_isha', 'Səfər: Məğrib və İşa', [('s_maghrib_isha', 'e'), ('s_maghrib_late', 'e'), ('s_how', 'e')]),
    ('safar_hadar', 'Səfər: şəhərdə birləşdirmək', [('s_hadar', 'e'), ('s_ibnabbas', 'e')]),
    ('safar_no_sunnah', 'Səfər: sünnət qılınmır', [('s_no_sunnah', 'e'), ('s_complete', 'e')]),
    ('safar_mina', 'Səfər: mukim imamın arxasında', [('s_mina_osman', 'e'), ('s_khilaf', 'e'), ('s_behind_imam', 'e')]),
    # Bayram
    ('eid_days', 'Bayram: iki gün', [('e_days', 'e')]),
    ('eid_musalla', 'Bayram: musalləyə çıxmaq', [('e_musalla', 'e')]),
    ('eid_no_adhan', 'Bayram: əzan və iqamə yoxdur', [('e_no_adhan', 'e'), ('e_no_iqama', 'e')]),
    ('eid_two', 'Bayram: iki rükət', [('e_two', 'e')]),
    ('eid_khutbah', 'Bayram: namazdan sonra xütbə', [('e_face', 'e'), ('e_standing', 'e')]),
    ('eid_sadaqa', 'Bayram: sədəqəyə çağırış', [('e_sadaqa', 'e')]),
    ('eid_first', 'Bayram: namaz xütbədən əvvəl', [('e_first', 'e')]),
    ('eid_marwan', 'Bayram: xütbəni qabağa keçirmək', [('e_marwan', 'e')]),
    ('eid_women', 'Bayram: qadınlar çıxsın', [('e_women_out', 'w'), ('e_hayd', 'w'), ('e_jilbab', 'w')]),
    ('eid_women_sadaqa', 'Bayram: qadınların sədəqəsi', [('e_women_sadaqa', 'w'), ('e_jewels', 'w'), ('e_family', 'w')]),
    # İstisqa
    ('istisqa_ask', 'İstisqa: quraqlıq ili', [('i_ask', 'e'), ('i_rain', 'e')]),
    ('istisqa_out', 'İstisqa: musalləyə çıxmaq', [('i_out', 'e')]),
    ('istisqa_back', 'İstisqa: qibləyə dönüb dua', [('i_back', 'e')]),
    ('istisqa_hands', 'İstisqa: əllərin qaldırılması', [('i_armpit', 'e'), ('i_palms', 'e')]),
    ('istisqa_pray', 'İstisqa: iki rükət uca səslə', [('i_then_pray', 'e')]),
    ('istisqa_jumuah', 'İstisqa: Cümə xütbəsində', [('i_jumua', 'e')]),
    ('istisqa_dua', 'İstisqa: dualar', [('isqina', 'd'), ('hawalayna', 'd')]),
    ('istisqa_only', 'Dua: istisqada əllər', [('i_only', 'e')]),
    ('istisqa_other', 'Dua: başqa dualarda əllər', [('i_other', 'e'), ('i_abubakr', 'e')]),
    # Küsuf
    ('kusuf_signs', 'Küsuf: iki ayət', [('k_signs', 'e'), ('k_ibrahim', 'e')]),
    ('kusuf_how', 'Küsuf: qılınışı', [('k_how', 'e'), ('k_second', 'e'), ('k_four', 'e'), ('k_baqara', 'e'), ('k_sajda', 'e'), ('k_rows', 'e')]),
    ('kusuf_until', 'Küsuf: açılanadək namaz', [('k_until', 'e')]),
    ('kusuf_do', 'Küsuf: dua, təkbir, sədəqə', [('k_do', 'e')]),
    ('kusuf_zikr', 'Küsuf: zikr', [('k_zikr', 'e')]),
    ('kusuf_itq', 'Küsuf: qul azad etmək', [('k_itq', 'e')]),
    ('kusuf_khutbah', 'Küsuf: namazdan sonra xütbə', [('k_khutbah', 'e')]),
    ('kusuf_women', 'Küsuf: qadınlar namazda', [('k_asma', 'w')]),
    # Qorxu namazı
    ('khawf_1', 'Qorxu: 1 + 1', [('f1_one', 'e'), ('f1_each', 'e')]),
    ('khawf_2', 'Qorxu: növbəli keşik', [('f2_guard', 'e')]),
    ('khawf_3', 'Qorxu: düşmən qiblədə', [('f3_qibla', 'e'), ('f3_salam', 'e')]),
    ('khawf_4', 'Qorxu: imam dörd', [('f4_four', 'e')]),
    ('khawf_5', 'Qorxu: hər dəstə bir', [('f5_one', 'e'), ('f5_qasr', 'e')]),
    ('khawf_severe', 'Qorxu: çox şiddətli olanda', [('f1_severe', 'e')]),
]
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
                z = ZK[src]; vol, no = z['ref']; h = H[(vol, no)]
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
open(f'{OUT}/salah3_seed.sql', 'w').write(sql)
vals = ','.join(f"('{k}',{s},'{a}','{z}')" for k, s, a, z in checks)
open(f'{OUT}/salah3_verify.sql', 'w').write(
    "select count(*) as expected, count(e.id) as found, count(*) filter (where md5(e.text_ar) = v.ar) as ar_ok, "
    "count(*) filter (where md5(e.text_az) = v.az) as az_ok, string_agg(case when e.id is null or md5(e.text_ar) <> v.ar or md5(e.text_az) <> v.az "
    "then v.topic || '/' || v.s end, ', ') as bad from (values " + vals + ") v(topic, s, ar, az) "
    "left join public.salah_evidence e on e.topic = v.topic and e.sort_no = v.s")
kt = '\n'.join(f'    {k.upper()}("{k}", "{title}"),' for k, title, _ in TOPICS)
open(f'{OUT}/salah3_topics.kt.txt', 'w').write(kt)
print(len(TOPICS), 'mövzu', len(rows), 'sətir', len(sql), 'bayt')
