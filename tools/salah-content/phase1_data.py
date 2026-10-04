"""Builds verified data for the Phase 1 (Təharət) maket from the 2026-10-03 content backup.

Every excerpt is checked to be a literal substring of the hadith's Azerbaijani text, so the
maket never shows wording that is not in the book.
"""
import json, re, sys, unicodedata

S = '/private/tmp/claude-501/-Users-macbook-Desktop-AnaMuslim/aa615236-1c22-4cdd-8c1d-03fdc4b14fcc/scratchpad'
d = json.load(open(f'{S}/db.json'))
H = d['hadith']
ch = {x['slug']: x for x in d['hadith_chapter']}
sc = {x['slug']: x for x in d['hadith_sub_chapter']}
bk = {x['slug']: x for x in d['hadith_book']}


def clean(t):
    return re.sub(r'\s+', ' ', (t or '').replace('­', '')).strip()


def vol(x):
    c = ch.get(x['chapter_slug'])
    return bk[c['book_slug']]['volume_slug'] if c else None


def num(x):
    m = re.match(r'\s*_?\s*(\d+)', x['text_az'] or '')
    return int(m.group(1)) if m else None


BY = {}
for x in H:
    v, n = vol(x), num(x)
    if v and n:
        BY.setdefault(('' if v == 'c1' else v + ':') + str(n), x)


def main_text(x):
    return clean(re.split(r'\n\s*(Digər|Başqa) (bir )?rəvayətdə', x['text_az'])[0])


def full_text(x):
    return clean(x['text_az'])


def ev_entry(k):
    x = BY[k]
    main = main_text(x)
    mm = re.search(r'[:,]\s*["“{]', main)
    st = mm.end() - 1 if mm else 0
    src = re.search(r'\((?:[^()]|\([^()]*\))*?-\s?\d[\d, -]*\)', main[st:])
    body = (main[st:st + src.start()] if src else main[st:]).strip().strip('"“”').strip()
    if len(body) > 620:
        body = body[:620]
        body = body[:body.rfind(' ')] + ' …'
    c = ch[x['chapter_slug']]
    s = sc.get(x['sub_chapter_slug'] or '')
    return {'id': x['id'], 'no': num(x), 'vol': vol(x), 'book': bk[c['book_slug']]['name'].rstrip('.'),
            'bab': clean((s or c)['name']), 'q': body, 'src': src.group(0).strip('()') if src else ''}


# --- Excerpts shown inside steps: (hadith key, literal substring) ---------------------------
X = {
    # Wudu, one-time form (№ 114) — begins with the face, as the hadith does.
    'w1_mouth': ('114', 'üzünü yudu, sonra (bir əlinə) bir ovuc su aldı və onunla həm madmada etdi, həm də istinşaq etdi'),
    'w1_face': ('114', 'Sonra bir ovuc su aldı və onu belə etdi;(su olan sağ ovcunu) sol ovcu ilə birləşdirdi və o ikisi (iki ovcu) ilə üzünü yudu'),
    'w1_rarm': ('114', 'Sonra bir ovuc su aldı və onunla sağ əlini (qolunu) yudu'),
    'w1_larm': ('114', 'Sonra bir ovuc su aldı və onunla sol əlini (qolunu) yudu'),
    'w1_head': ('114', 'Sonra da başına məsh çəkdi'),
    'w1_rfoot': ('114', 'Sonra sudan bir ovuc da aldı və (az-az axıdaraq) sağ ayağını yudu'),
    'w1_lfoot': ('114', 'Sonra da başqa bir ovuc su aldı və onunla ayağını-yəni sol ayağını-yudu'),
    'w1_end': ('114', 'Rəsulullah sallallahu aleyhi və səlləmi belə dəstəmaz alarkən gördüm'),
    'w1_113': ('113', 'bir dəfə bir dəfə (yumaqla) dəstəmaz aldı'),
    # Wudu, full form (№ 112)
    'w3_hands': ('112', 'əllərini iki dəfə yudu'),
    'w3_mouth': ('112', 'Sonra üç dəfə madmada etdi (ağzını yaxaladı) və istinsar etdi (burnuna su çəkib sonra tökdü)'),
    'w3_face': ('112', 'Sonra üzünü üç dəfə yudu'),
    'w3_arms': ('112', 'Sonra hər iki əlini dirsəklərinə kimi (dirsəkləri ilə birlikdə) iki dəfə yudu'),
    'w3_head': ('112', 'Başının ön tərəfindən başlayıb əllərini başının arxasına apardı, sonra əllərini (məshə) başladığı yerə qaytardı'),
    'w3_feet': ('112', 'Sonra da ayaqlarını yudu'),
    'w_sleep': ('125', 'dəstəmaz suyu olan qaba əlini salmazdan əvvəl qoy əlini yusun'),
    'w_nose': ('115', 'burnuna su alsın, sonra (o suyu) burnundan (geri) töksün'),
    'w_right': ('109', 'gücü çatdığı qədər təyəmmunu (sağdan başlamağı) sevərdi'),
    'w_heels': ('111', 'Odda yanacaq dabanların vay halına!'),
    'w_mudd': ('110', 'bir məkukla isə dəstəmaz alırdı'),
    'w_many': ('118', 'Dəstəmazını pozmadıqca, hər birimizə (aldığı) dəstəmaz kifayət edirdi'),
    'w_doubt': ('119', 'səsini eşitmədikcə və ya qoxusunu hiss etmədikcə, ona heç bir şey vacib deyil'),
    'w_madhy': ('137', 'Onu gördükdə dəstəmaz al və onu yu'),
    'w_meat': ('128', 'dəstəmaz almadan namaz qıldı'),
    'w_milk': ('126', 'Həqiqətən də onun (südün) yağı var'),
    'w_eat': ('98', 'onu yedi və suya əl vurmadı (dəstəmaz almadı)'),
    'w_miswak': ('122', 'onlara (hər) dəstəmazda sivakı əmr edərdim'),
    # Ghusl (№ 140, 141, 142, 145, 146)
    'g_hands': ('140', 'qaba əlini salmazdan əvvəl əllərini yumaqla başlayardı'),
    'g_private': ('140', 'Sonra övrət yerini yuyur'),
    'g_wudu': ('140', 'sonra isə namaz üçün aldığı kimi dəstəmaz alırdı'),
    'g_hair': ('140', 'Sonra saçlarını su ilə isladar'),
    'g_three': ('140', 'sonra da başına üç ovuc su tökərdi'),
    'g_sides': ('141', 'başının sağ tərəfindən (yuyunmağa) başlayar, sonra (da başının) sol tərəfini (su alaraq) yuyar. Sonra da iki ovucu ilə başının ortasına su tökərdi'),
    'g_body': ('145', 'başına üç ovuc su töküb sonra da (bütün bədəninə) su tökmək bəs edər'),
    'g_braids': ('145', 'cənabət qüslü alanda onu açımmı?'),
    'g_wet': ('146', 'Bəli, əgər su görsə'),
    'g_noinzal1': ('134', 'Ondan qadına dəyəni yuyur, dəstəmaz alır və namazını qılır'),
    'g_noinzal2': ('135', 'Tələsdirildiyin və yaxud məni axıtmadığın zaman sənə qüsl yoxdur, sənə (yalnız) dəstəmaz almaq gərəklidir'),
    'g_saa': ('143', 'Sənə bir saa\' (su) kifayət edər'),
    'g_friday': ('437', 'Cümə gününün qüslü bütün (hər bir) muhtəlimin (həddi-buluğa çatmış müsəlmanın) üzərinə vacibdir'),
    'g_sleep': ('150', 'övrət yerini yuyar və namaz üçün dəstəmaz aldığı kimi dəstəmaz alardı'),
    # Tayammum (№ 180, 177, 179, 181, 182)
    't_strike': ('180', 'hər iki ovcunu yerə vurdu'),
    't_blow': ('180', 'sonra onlara üfürdü'),
    't_wipe': ('180', 'sonra o ikisi ilə üzünə və qollarının bir hissəsinə (biləklərinə) məsh etdi'),
    't_enough': ('180', 'Təmiz torpaq sənə kifayət edərdi'),
    't_junub': ('181', 'Sən torpağa (təyəmmüm etməyə) yönəl, (çünki,) o sənə kifayət edir'),
    't_wall': ('182', 'divara yönəldi (əllərini ona sürtüb) üzünü və iki əlini məsh etdi'),
    't_earth': ('177', 'Mənim üçün yer üzü tahur (təmiz, pak, namaz qılmaq üçün yararlı) və məscid'),
    # Khuff (№ 130–133)
    'k_wipe': ('131', 'O, dəstəmaz aldı və xufflarına məsh çəkdi'),
    'k_time': ('133', 'üç gün və gecələrini müsafir üçün, muqim (səfərdə olmayan kimsə) üçün isə bir gün və bir gecəni (icazəli) etdi'),
    'k_last': ('132', 'Rəsulullah sallallahu aleyhi və səlləmi bunun mislində (əməl) etdiyini gördüm'),
    # Toilet manners (№ 91–99)
    'a_dua': ('91', 'Əuuzu billəhi minəl-xubsi val-xabəəisi'),
    'a_qibla': ('94', 'üzü qibləyə tərəf durmayın, lakin şərqə və ya qərbə tərəf (1) durun'),
    'a_house': ('95', 'arxası Beytə (Kəbəyə), üzü Şama doğru ehtiyacını dəf etdiyini gördüm'),
    'a_right': ('92', 'bizdən birinin sağ əliylə istinca etməsini'),
    'a_three': ('92', 'Sizdən biriniz üçdən az daşla istinca etməsin'),
    'a_bone': ('92', 'bizə peyin və sümüyü (onlarla təmizlənməyi) qadağan etmişdir'),
    'a_odd': ('115', 'Kim də isticmar (təbii ehtiyacını dəf etdikdən sonra daşla təmizlənmək) etsə, tək (sayda daşla) etsin'),
    'a_still': ('99', 'Sizdən heç kim əsla axmayan suya bövl etməsin'),
    'a_stand': ('97', 'ayaq üstə bövl etdi'),
    # Najasa
    'n_baby': ('105', 'suyu sidiyin (axdığı yerlərin) üzərinə səpdi'),
    'n_mosque': ('103', 'sidiyinin üstünə bir vedrə su və ya bir çəllək su tökün'),
    'n_ease': ('103', 'Çünki siz asanlaşdıranlar olaraq göndərildiniz, çətinləşdirənlər olaraq göndərilmədiniz'),
    'n_dog': ('107', 'onun təmizliyi onu yeddi dəfə yumaqdır, (o yeddi dəfə yumağın) birincisi torpaqladır'),
    'n_blood': ('166', 'onu dırnaqları (və ya barmaqları) ilə ovuşdursun, sonra su tökə-tökə onu yusun, sonra da onunla namaz qılsın'),
    'n_mani': ('147', 'o da onu paltarından yuyurdu, sonra da (elə həmin) paltarında namaza çıxırdı'),
    'n_mani2': ('148', 'Rəsulullah sallallahu aleyhi və səlləmin paltarından ovmaqdan (ovxalayaraq təmizləməkdən) daha artığını etmirdim'),
    'n_muslim': ('158', 'Şübhəsiz ki, mömin nəcis olmur'),
    # Women (inline notes)
    'f_prayer': ('161', 'O, gecələri qalır və namaz qılmır və Ramazanda iftar edir'),
    'f_qada': ('162', 'Biz Rəsulullah sallallahu aleyhi və səlləmin zamanında heyz olurduq, məgər biz o zaman qəza edirdik?'),
    'f_istihada': ('163', 'O da ona (adəti üzrə gördüyü heyz günləri bitdikdən sonra) qüsl alıb namaz qılmasını əmr etdi'),
    't_nowater': ('180', 'biz bir ay, iki ay qalırıq və (qüsl etmək, dəstəmaz almaq üçün) su tapmırıq'),
    'toilet_dua_tr': ('91', 'Allahummə! İnnii əuuzu bikə minəl-xubusi val-xabəəisi'),
    'f_yellow': ('164', 'sarı və bulanıq ifrazatı bir şey hesab etməzdik'),
    'f_ghusl': ('165', 'Onunla qan iz(lər)ini sil'),
    'f_eid': ('174', 'heyzli qadınlar namazdan uzaq durmalıdırlar'),
}

EXCERPTS = {}
bad = []
for k, (hk, sub) in X.items():
    x = BY[hk]
    t = full_text(x)
    if sub not in t:
        bad.append((k, hk, sub))
    else:
        EXCERPTS[k] = {'ref': hk, 'text': sub}
if bad:
    for b in bad:
        print('NOT FOUND', b, file=sys.stderr)
    sys.exit(1)


# --- Arabic helpers ---------------------------------------------------------------------------
D = '[ً-ْٰـ]*'


def ar(hk, a, b):
    t = unicodedata.normalize('NFKC', BY[hk]['text_ar'])
    pa = lambda w: r'\s*'.join(D.join('[اأإآٱ]' if c == 'ا' else re.escape(c) for c in part) + D for part in w.split())
    m = re.search(pa(a), t)
    e = re.search(pa(b), t[m.start():])
    return re.sub(r'،(?=\S)', '، ', t[m.start():m.start() + e.end()]).replace('\n', ' ')


def note_meaning(hk):
    n = clean(BY[hk]['note'])
    m = re.search(r'belədir:\s*["{“]?', n)
    return n[m.end():].split('" ->')[0].strip().rstrip('"”').strip()


ZK = {
    'toilet': {'ar': ar('91', 'اللهم اني اعوذ', 'الخبائث'),
               'tr': 'Allahummə! İnnii əuuzu bikə minəl-xubusi val-xabəəisi',
               'mean': note_meaning('91'), 'ref': '91'},
}
assert ZK['toilet']['tr'] in full_text(BY['91'])

# --- Adhan: user-supplied wording, Arabic from the book where it exists -----------------------
TASH = note_meaning('359')
TAHL = note_meaning('369')
ADHAN = [
    {'tr': 'Allahu Əkbər', 'ar': 'اللَّهُ أَكْبَرُ', 'arSrc': None, 'mean': 'Allah ən böyükdür', 'meanSrc': None, 'adhan': 2, 'iqama': 1},
    {'tr': 'Əşhədu əllə iləhə illəllah', 'ar': ar('226', 'اشهد ان لا', 'الله'), 'arSrc': '226',
     'mean': 'Şahidlik edirəm ki, Allahdan başqa ilah yoxdur', 'meanSrc': '359', 'adhan': 2, 'iqama': 1},
    {'tr': 'Əşhədu ənnə Muhəmmədən rasulullah', 'ar': ar('226', 'اشهد ان محمدا', 'الله'), 'arSrc': '226',
     'mean': 'Şahidlik edirəm ki, Muhəmməd Allahın Rəsuludur', 'meanSrc': '359', 'adhan': 2, 'iqama': 1},
    {'tr': 'Həyyə aləs-saləh', 'ar': ar('226', 'حي على', 'الصلاة'), 'arSrc': '226', 'mean': 'Namaza gəlin', 'meanSrc': None, 'adhan': 2, 'iqama': 1},
    {'tr': 'Həyyə aləl-fələh', 'ar': ar('306', 'حي على الفلاح', 'الفلاح'), 'arSrc': '306', 'mean': 'Qurtuluşa gəlin', 'meanSrc': None, 'adhan': 2, 'iqama': 1},
    {'tr': 'Qad-qamətis - saləh', 'ar': 'قَدْ قَامَتِ الصَّلَاةُ', 'arSrc': None, 'mean': 'Namaz başladı', 'meanSrc': None, 'adhan': 0, 'iqama': 2, 'trSrc': '307'},
    {'tr': 'Allahu Əkbər', 'ar': 'اللَّهُ أَكْبَرُ', 'arSrc': None, 'mean': 'Allah ən böyükdür', 'meanSrc': None, 'adhan': 2, 'iqama': 1},
    {'tr': 'Lə iləhə illəllah', 'ar': ar('369', 'لا اله الا الله', 'الله'), 'arSrc': '369', 'mean': 'Allahdan başqa ilah yoxdur', 'meanSrc': '369', 'adhan': 2, 'iqama': 1},
]
assert 'Allahdan başqa ilah yoxdur' in TASH and 'Allahdan başqa ilah yoxdur' in TAHL
assert 'Rəsuludur' in TASH
assert 'qad qamətis-saləh' in clean(BY['307']['text_az'])

refs = {v['ref'] for v in EXCERPTS.values()} | {z['ref'] for z in ZK.values()}
refs |= {a['arSrc'] for a in ADHAN if a['arSrc']} | {a['meanSrc'] for a in ADHAN if a['meanSrc']} | {'307', '306', '134', '135', '159', '160', '168', '173', '176', '178', '179', '100', '104', '106', '108', '120', '124', '130', '138', '139', '142', '144', '149', '153', '157', '93', '96', '129', '136', '127', '113'}
EV = {k: ev_entry(k) for k in sorted(refs, key=lambda s: (len(s), s))}

json.dump({'EV': EV, 'EX': EXCERPTS, 'ZK': ZK, 'ADHAN': ADHAN}, open(f'{S}/phase1.json', 'w'), ensure_ascii=False)
print('excerpts', len(EXCERPTS), 'ev', len(EV))
for a in ADHAN:
    print(a['tr'], '|', a['ar'])
print(ZK['toilet'])
