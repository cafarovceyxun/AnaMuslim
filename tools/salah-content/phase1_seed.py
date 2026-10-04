"""Single source for Phase 1 topics: emits the Kotlin SalahTopic enum and the salah_evidence seed SQL."""
import json, sys

S = '/private/tmp/claude-501/-Users-macbook-Desktop-AnaMuslim/aa615236-1c22-4cdd-8c1d-03fdc4b14fcc/scratchpad'
exec(open(f'{S}/phase1_data.py').read().split('# --- Excerpts shown')[0])
EX = json.load(open(f'{S}/phase1.json'))['EX']
AR = json.load(open(f'{S}/phase1_ar.json'))

# (key, picker title, [(excerpt key, kind)]) — kind: e = evidence, w = women, d = dhikr
TOPICS = [
    ('wudu1_mouth', 'Dəstəmaz, bir dəfə: ağız və burun', [('w1_mouth', 'e'), ('w_nose', 'e')]),
    ('wudu1_face', 'Dəstəmaz, bir dəfə: üz', [('w1_face', 'e')]),
    ('wudu1_rarm', 'Dəstəmaz, bir dəfə: sağ qol', [('w1_rarm', 'e'), ('w_right', 'e')]),
    ('wudu1_larm', 'Dəstəmaz, bir dəfə: sol qol', [('w1_larm', 'e')]),
    ('wudu1_head', 'Dəstəmaz, bir dəfə: başa məsh', [('w1_head', 'e')]),
    ('wudu1_rfoot', 'Dəstəmaz, bir dəfə: sağ ayaq', [('w1_rfoot', 'e'), ('w_heels', 'e')]),
    ('wudu1_lfoot', 'Dəstəmaz, bir dəfə: sol ayaq', [('w1_lfoot', 'e'), ('w1_end', 'e')]),
    ('wudu3_hands', 'Dəstəmaz, tam forma: əllər', [('w3_hands', 'e'), ('w_sleep', 'e')]),
    ('wudu3_mouth', 'Dəstəmaz, tam forma: ağız və burun', [('w3_mouth', 'e'), ('w_nose', 'e')]),
    ('wudu3_face', 'Dəstəmaz, tam forma: üz', [('w3_face', 'e')]),
    ('wudu3_arms', 'Dəstəmaz, tam forma: qollar', [('w3_arms', 'e'), ('w_right', 'e')]),
    ('wudu3_head', 'Dəstəmaz, tam forma: başa məsh', [('w3_head', 'e')]),
    ('wudu3_feet', 'Dəstəmaz, tam forma: ayaqlar', [('w3_feet', 'e'), ('w_heels', 'e')]),
    ('wudu_forms', 'Dəstəmaz: hər iki forma', [('w1_113', 'e')]),
    ('wudu_doubt', 'Dəstəmaz: qaz və şübhə', [('w_doubt', 'e')]),
    ('wudu_madhy', 'Dəstəmaz: məzi', [('w_madhy', 'e')]),
    ('wudu_meat', 'Dəstəmaz: ət yemək', [('w_meat', 'e')]),
    ('wudu_milk', 'Dəstəmaz: süd içmək', [('w_milk', 'e')]),
    ('wudu_food', 'Dəstəmaz: ayaqyolundan sonra yemək', [('w_eat', 'e')]),
    ('wudu_miswak', 'Dəstəmaz: misvak', [('w_miswak', 'e')]),
    ('wudu_right', 'Dəstəmaz: sağdan başlamaq', [('w_right', 'e')]),
    ('wudu_water', 'Dəstəmaz: suyun miqdarı', [('w_mudd', 'e')]),
    ('wudu_many', 'Dəstəmaz: bir dəstəmazla neçə namaz', [('w_many', 'e')]),
    ('ghusl_wet', 'Qüsl: ehtilam', [('g_wet', 'e')]),
    ('ghusl_friday', 'Qüsl: cümə günü', [('g_friday', 'e')]),
    ('ghusl_noinzal', 'Qüsl: məni axmayanda', [('g_noinzal1', 'e'), ('g_noinzal2', 'e')]),
    ('ghusl_hands', 'Qüsl: əllər', [('g_hands', 'e')]),
    ('ghusl_private', 'Qüsl: övrət yeri', [('g_private', 'e')]),
    ('ghusl_wudu', 'Qüsl: namaz dəstəmazı', [('g_wudu', 'e')]),
    ('ghusl_hair', 'Qüsl: saçı isladmaq', [('g_hair', 'e')]),
    ('ghusl_three', 'Qüsl: başa üç ovuc', [('g_three', 'e')]),
    ('ghusl_sides', 'Qüsl: sağ, sol, orta', [('g_sides', 'e')]),
    ('ghusl_body', 'Qüsl: bütün bədən', [('g_body', 'e'), ('g_braids', 'w')]),
    ('ghusl_water', 'Qüsl: suyun miqdarı', [('g_saa', 'e')]),
    ('ghusl_sleep', 'Qüsl: cünubun yatması', [('g_sleep', 'e')]),
    ('ghusl_hayd', 'Qüsl: heyzdən sonra', [('f_ghusl', 'w')]),
    ('taharah_believer', 'Təharət: mömin nəcis olmur', [('n_muslim', 'e')]),
    ('tayammum_strike', 'Təyəmmüm: ovucları yerə vurmaq', [('t_strike', 'e')]),
    ('tayammum_blow', 'Təyəmmüm: üfürmək', [('t_blow', 'e')]),
    ('tayammum_face', 'Təyəmmüm: üzə məsh', [('t_wipe', 'e')]),
    ('tayammum_hands', 'Təyəmmüm: biləklərə məsh', [('t_wipe', 'e')]),
    ('tayammum_enough', 'Təyəmmüm: təmiz torpaq kifayətdir', [('t_enough', 'e')]),
    ('tayammum_nowater', 'Təyəmmüm: su olmayanda', [('t_nowater', 'e')]),
    ('tayammum_junub', 'Təyəmmüm: cünub üçün', [('t_junub', 'e')]),
    ('tayammum_wall', 'Təyəmmüm: divarla', [('t_wall', 'e')]),
    ('tayammum_earth', 'Təyəmmüm: yer üzü təmizdir', [('t_earth', 'e')]),
    ('khuff_wipe', 'Xuff: məsh', [('k_wipe', 'e'), ('k_last', 'e')]),
    ('khuff_time', 'Xuff: müddət', [('k_time', 'e')]),
    ('toilet_dua', 'Ayaqyolu: giriş duası', [('toilet_dua_tr', 'd')]),
    ('toilet_qibla', 'Ayaqyolu: qiblə', [('a_qibla', 'e')]),
    ('toilet_house', 'Ayaqyolu: bina içində', [('a_house', 'e')]),
    ('toilet_right', 'Ayaqyolu: sağ əl', [('a_right', 'e')]),
    ('toilet_stones', 'Ayaqyolu: daşla təmizlənmə', [('a_three', 'e'), ('a_odd', 'e')]),
    ('toilet_bones', 'Ayaqyolu: peyin və sümük', [('a_bone', 'e')]),
    ('toilet_still', 'Ayaqyolu: durğun su', [('a_still', 'e')]),
    ('toilet_standing', 'Ayaqyolu: ayaq üstə', [('a_stand', 'e')]),
    ('najasa_baby', 'Nəcasət: uşaq sidiyi', [('n_baby', 'e')]),
    ('najasa_floor', 'Nəcasət: yerdəki sidik', [('n_mosque', 'e'), ('n_ease', 'e')]),
    ('najasa_dog', 'Nəcasət: itin yaladığı qab', [('n_dog', 'e')]),
    ('najasa_blood', 'Nəcasət: heyz qanı', [('n_blood', 'e')]),
    ('najasa_mani', 'Nəcasət: məni', [('n_mani', 'e'), ('n_mani2', 'e')]),
    ('hayd_prayer', 'Heyz: namaz və oruc', [('f_prayer', 'w')]),
    ('hayd_qada', 'Heyz: qəza', [('f_qada', 'w')]),
    ('hayd_istihada', 'Heyz: istihazə', [('f_istihada', 'w')]),
    ('hayd_discharge', 'Heyz: rəngli ifrazat', [('f_yellow', 'w')]),
    ('hayd_eid', 'Heyz: bayram namazı', [('f_eid', 'w')]),
]
KIND = {'e': 'evidence', 'w': 'women', 'd': 'dhikr'}
TOILET_MEANING = 'Allahummə! Erkək və dişi şeytanlardan Sənə sığınıram!'
assert TOILET_MEANING in BY['91']['note']


def lit(s):
    return "'" + s.replace("'", "''") + "'" if s is not None else 'null'


rows = []
for key, _, items in TOPICS:
    for i, (ex, k) in enumerate(items):
        hk = EX[ex]['ref']
        h = BY[hk]
        ar = AR[ex]
        az = EX[ex]['text']
        tr = None
        if k == 'd':
            tr, az = az, TOILET_MEANING
        assert ar in h['text_ar'], (ex, 'arabic not a raw substring')
        rows.append(f"({lit(key)}, {lit(KIND[k])}, 'hadith', {h['id']}, {lit(ar)}, {lit(az)}, {lit(tr)}, "
                    f"{lit('Muheymin 1-ci cild, № ' + hk)}, {i * 10})")
sql = ('insert into public.salah_evidence (topic, kind, source_type, hadith_id, text_ar, text_az, transliteration, source, sort_no) values\n'
       + ',\n'.join(rows) + '\non conflict do nothing;\n')
open(f'{S}/salah_seed.sql', 'w').write(sql)

kt = '\n'.join(f'    {k.upper()}("{k}", "{t}"),' for k, t, _ in TOPICS)
open(f'{S}/salah_topics.kt.txt', 'w').write(kt)
print(len(TOPICS), 'topics', len(rows), 'rows', len(sql), 'bytes')
