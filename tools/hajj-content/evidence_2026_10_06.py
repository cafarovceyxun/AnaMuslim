"""2026-10-06: Həcc bələdçisinin dəlilləri — Muheymin 2, «Həcc kitabı»nın (№ 776–929) tam yoxlamasından sonra.

python3 evidence_2026_10_06.py <yedək.json> <çıxış qovluğu> [--print]
Çıxış: hajj_seed.sql, hajj_verify.sql.

Üsul namaz bələdçisi ilə eynidir (`tools/salah-content/fixes-2026-10-05/fixes.py`), bir fərqlə: azərbaycanca
çıxarış da əl ilə yazılmır, **başlanğıc və son ifadə** ilə hədisin öz `text_az`-ından kəsilir — köçürmə
xətası ola bilməsin. Ərəbcə SQL-də ötürülmür, server `substr(h.text_ar, …)` ilə kəsir.

Qaydalar:
- `{{ }}` (Peyğəmbərin sözü işarəsi) çıxarışa düşməməlidir — kart mətni xam göstərir. Ona görə ya yalnız
  mötərizənin içi, ya da yalnız rəvayətçinin sözü götürülür.
- Hər sətir kitabda **hədisin özündən** gəlir; bab adı dəlil deyil.
"""
import json, re, sys, os, hashlib

P = os.path.dirname(os.path.abspath(__file__))
SRC, OUT = sys.argv[1], sys.argv[2]
PRINT = '--print' in sys.argv
exec(open(f'{P}/../salah-content/phase2/phase2_ar.py').read().split('# Çıxarış açarı')[0])

d = json.load(open(SRC))
t = d.get('tables', d)
H = {}
for r in t['hadith']:
    if 986 <= r['id'] <= 1146:  # Muheymin 2 → Həcc kitabı
        m = re.match(r'[\s_]*(\d+)\.', r['text_az'] or '')
        H[int(m[1])] = r

# Kitabdakı tərcümə səhvləri — seed-dən əvvəl bazada düzəldilir (uzunluq dəyişmir, mövqelər sürüşmür).
# № 858: ərəbcə «فَلْيَصُمْ ثَلَاثَةَ أَيَّامٍ فِي الْحَجِّ وَسَبْعَةً إِذَا رَجَعَ» — Həcdə üç, qayıdanda yeddi; tərcümədə tərs idi.
TEXT_FIXES = {
    858: [('Həcdə yeddi gün, əhlinin yanına qayıtdıqda isə üç gün', 'Həcdə üç gün, əhlinin yanına qayıtdıqda isə yeddi gün')],
}
for _no, _pairs in TEXT_FIXES.items():
    for _old, _new in _pairs:
        assert len(_old) == len(_new) and _old in H[_no]['text_az']
        H[_no]['text_az'] = H[_no]['text_az'].replace(_old, _new)

# (mövzu, №, (az başlanğıc, az son[, az-dan əvvəlki işarə]), (ərəbcə lövbərlər), sort_no)
# az son None → yalnız başlanğıc ifadəsinin özü.
ROWS = [
    # --- Fəzilət ---
    ('virtue', 776, ('Şübhəsiz ki, İslam beş şey', 'Beyti həcc etmək'), ('ان الاسلام بني على خمس', 'وحج البيت'), -10),
    ('virtue', 781, ('Sənin üçün cihadın ən yaxşısı', 'məbrur Həcc'), ('لك احسن الجهاد', 'حج مبرور'), 3),

    # --- Kim Həcc edir (yeni) ---
    ('who', 782, ('Ey insanlar! Şübhəsiz ki, Allah sizə həcci fərz etmişdir', None), ('يا ايها الناس', 'فرض عليكم الحج'), 0),
    ('who', 782, ('Əgər "bəli" desəydim', 'siz onu etməyəcəkdiniz'), ('لو قلت نعم', 'لما قمتم به'), 10),
    ('who', 788, ('(Müsəlman) qadın, onunla birlikdə məhrəmi', 'onun yanına daxil olmur'), ('لا تسافر امراه', 'وعندها ذو محرم'), 20),
    ('who', 786, ('Atanın yerinə Həcc və ya Ümrə et!', None), ('حج عن ابيك او اعتمر',), 30),
    ('who', 784, ('Vida Həccində Rəsulullah sallallahu aleyhi və səlləm ilə birlikdə mənə Həcc etdirildi', 'yeddi yaşım vardı'),
     ('حج بي مع رسول الله', 'ابن سبع سنين'), 40),

    # --- İhram ---
    ('ihram', 790, ("Kim nə'l (altı qalın", 'qoy şalvar geyinsin'), ('من لم يجد نعلين', 'فليلبس سراويل'), 3),
    ('ihram', 791, ('Mən, Rəsulullah sallallahu aleyhi və səlləmə, ihrama girməmişdən öncə', 'ətir vururdum'),
     ('كنت اطيب رسول الله', 'قبل ان يطوف بالبيت'), 4),
    ('ihram', 793, ('Rəsulullah sallallahu aleyhi və səlləm muhrim ikən evləndi', None), ('ان رسول الله', 'تزوج وهو محرم'), 5),
    ('ihram', 794, ('Rəsulullah sallallahu aleyhi və səlləm muhrim ikən hicama etdirdi', None), ('ان رسول الله', 'احتجم وهو محرم'), 6),
    ('ihram', 796, ('O da onun başına su tökdü', 'belə etdiyini gördüm'), ('فصب على راسه', 'يفعل'), 7),
    ('ihram', 797, ('(Elə isə) başını qırx', 'bir qurban kəs'), ('فاحلق راسك', 'نسكا'), 8),
    ('ihram', 801, ('Biz onu sənə yalnız muhrimlər olduğumuz üçün geri verdik', None), ('انا لم نرده عليك', 'حرم'), 10),
    ('ihram', 929, ('Səndəki ətirə gəldikdə, onu üç dəfə yu', 'onu da çıxar'), ('اما الطيب الذي بك', 'فانزعها'), 9),
    ('ihram', 868, ('Onu su və sidr ilə yuyun', 'olaraq dirildiləcəkdir'), ('اغسلوه بماء وسدر', 'ملبيا'), 11),

    # --- Şərtli ihram və maneə (yeni) ---
    ('condition', 837, ('Həcc et və şərt qoş', 'ihramdan çıxdığım yerdir'), ('حجي واشترطي', 'حيث حبستني'), 0),
    ('condition', 856, ('Rəsulullah  sallallahu aleyhi və səlləmin sünnəti sizə kifayət', 'oruc tutsun'), ('اليس حسبكم سنه رسول الله', 'ان لم يجد هديا'), 10),

    # --- Heyzli və nifaslı qadın (yeni) ---
    ('haidh', 859, ('Həqiqətən də, bu, Allahın Adəmin qızlarına', 'Beyti tavaf etmə'), ('ان هذا شيء كتبه الله', 'حتى تطهري'), 0),
    ('haidh', 861, ('Qüsl et, bir paltara bürün və təlbiyə et (ihrama gir)', None), ('اغتسلي', 'ثم اهلي'), 10),
    ('haidh', 858, ('Saçlarını aç, daran', 'Ümrəni (sonraya) burax'), ('انقضي راسك', 'العمره'), 20),

    # --- Məkkəyə giriş (yeni) ---
    ('makkah', 833, ('İbn Ömər harama yaxınlaşdıqda', 'sallallahu aleyhi və səlləm onu edirdi'), ('كان ابن عمر اذا دخل ادنى الحرم', 'كان يفعله'), 0),
    ('makkah', 834, ('O, Məkkəyə daxil olduqda yuxarı təpədən', 'aşağı təpədən çıxardı'), ('واذا دخل مكه', 'الثنيه السفلى'), 10),

    # --- Tavaf ---
    ('tawaf', 861, ('Rəsulullah sallallahu aleyhi və səlləmi gördüm;Həcərul-Əsvəddən', 'üç (dövrə) tavaf etdi'),
     ('رايت رسول الله', 'ثلاثه اطواف'), 4),
    ('tawaf', 846, ('Nəbi sallallahu aleyhi və səlləmin Beytdən iki Yəməni', 'görmədim'), ('لم ار النبي', 'اليمانيين'), 5),
    ('tawaf', 861, ('Sonra İbrahimin (aleyhissələm) məqamına getdi', 'Özü ilə Beytin arasında qoydu'),
     ('ثم نفذ الى مقام ابراهيم', 'بينه وبين البيت'), 6),
    ('tawaf', 861, ('Nəbi sallallahu aleyhi və səlləm tavafın iki rükətində', 'huvallahu əhəd}-i oxudu'),
     ('قرا في ركعتي الطواف', 'احد}'), 7),
    ('tawaf', 843, ('Miniyə minərək insanların arxasından tavaf et', None), ('طوفي من وراء الناس', 'راكبه'), 10),

    # --- Səy ---
    ('say', 861, ('Sonra O, rüknə tərəf qayıdıb onu istilam etdi', 'Səfaya tərəf çıxdı'), ('ثم رجع الى الركن فاستلمه', 'الى الصفا'), 0),

    # --- Qiran və İfradda bir səy (yeni) ---
    ('say_single', 858, ('Ümrə (niyyəti ilə) təlbiyə edənlər Beyti tavaf', 'yalnız bir tavafla tavaf etdilər'),
     ('فطاف الذين اهلوا بالعمره', 'طوافا واحدا'), 0),
    ('say_single', 857, ('sonra o ikisi (Həcc və Ümrə) üçün Beyti', 'belə qalmağa davam etdi'),
     ('ثم طاف لهما', 'يوم النحر'), 10),

    # --- Təlbiyə ---
    ('talbiyah', 819, ('Mən Əbu Talhənin tərkində idim', 'Həcci və Ümrəni'), ('كنت رديف ابي طلحه', 'والعمره'), 10),
    ('talbiyah', 829, ('Rəsulullah sallallahu aleyhi və səlləm ilə birlikdə (Həcc', 'biz də onu Ümrə etdik'),
     ('خرجنا مع رسول الله', 'فجعلناها عمره'), 11),
    ('talbiyah', 880, ('Nəbi sallallahu aleyhi və səlləm Cəməratul-Aqabədə daş atana qədər təlbiyə etdi', None),
     ('ان النبي', 'جمره العقبه', 'وفي روايه'), 12),

    # --- Həcc növləri ---
    ('types', 821, ('Bizdən kimisi Ümrəyə təlbiyə etdi', 'gününə qədər ihramdan çıxmadılar'), ('فمنا من اهل بعمره', 'يوم النحر'), 2),
    ('types', 858, ("Nəbi sallallahu aleyhi və səlləm Vida Həcci ilində Ümrə ilə birlikdə Həccə təməttu'", 'Zul-Huleyfədən (sürüb) apardı'),
     ('تمتع النبي', 'من ذي الحليفه'), 3),
    ('types', 858, ('Sizdən kim qurbanlıq heyvan gətiribsə', 'heç bir şeydə ihramdan çıxmır'), ('من كان منكم اهدى', 'حتى يقضي حجه'), 4),
    ('types', 820, ('Ləbbeykə bihəccətin va umratin məan!', None), ('لبيك بحجه وعمره معا',), 5),
    ('types', 823, ('Kimin yanında (kəsəcək) qurbanı yoxdursa', 'ihramda (qalmağa) davam etsin'), ('من لم يكن معه هدي', 'فليقم على احرامه'), 6),
    ('types', 830, ('Həqiqətən də, mən saçımı yapışdırmışam', 'qurban kəsənədək ihramdan çıxmayacağam'), ('اني لبدت راسي', 'حتى انحر'), 7),
    ('types', 861, ("Suraqa ibn Məlik ibn Cu'şum dedi", 'yoxsa əbədidir?"'), ('وقال سراقه', 'ام للابد'), 8),
    ('types', 861, ('Xeyr, əksinə, o, əbədidir. Ümrə Həccə daxil olmuşdur', None), ('لا،بل للابد', 'دخلت العمره في الحج'), 9),

    # --- Tərviyə günü və Mina ---
    ('tarwiya', 861, ('Tərviyə günü olduqda isə Minaya üz tutub', 'Sonra Günəş çıxanadək azca gözlədi'),
     ('فلما كان يوم الترويه توجهوا الى منى', 'حتى طلعت الشمس'), 2),
    ('tarwiya', 908, ('Sayımızın ən çox (olduğu)', 'iki rükət (olaraq) qıldırdı'), ('صلى بنا رسول الله', 'ركعتين'), 3),

    # --- Ərəfat ---
    ('arafah', 861, ('Sonra (Rəsulullah sallallahu aleyhi və səlləm müəzzinə', 'arasında heç bir şey qılmadı', 'Allahummə şahid ol!'),
     ('ثم اذن', 'ولم يصل بينهما شيئا', 'اللهم اشهد'), 3),
    ('arafah', 861, ('Sonra Rəsulullah sallallahu aleyhi və səlləm miniyinə minib Məvqifə', 'Günəş qürub edənə qədər vaqfədə qaldı'),
     ('ثم ركب رسول الله', 'حتى غربت الشمس'), 4),
    ('arafah', 865, ('Qureyş belə deyirdi', '(Bəqərə, 199)'), ('كانت قريش', 'افاض الناس}'), 5),
    ('arafah', 867, ('Əgər bu gün Sünnətə isabət etmək', 'namazı tezləşdir'), ('ان كنت تريد ان تصيب السنه', 'وعجل الصلاه'), 6),

    # --- Müzdəlifə ---
    ('muzdalifah', 861, ('Ey insanlar! Sükunət! Sükunət!', '(Sükunəti qoruyun!)'), ('ايها الناس،السكينه', 'السكينه السكينه'), -1),
    ('muzdalifah', 870, ('Muzdəlifəyə gələndə (çatanda) endi', 'arasında başqa heç bir şey (namaz) qılmadı'),
     ('فلما جاء المزدلفه نزل', 'ولم يصل بينهما شيئا'), 0),
    ('muzdalifah', 861, ('Sonra Rəsulullah sallallahu aleyhi və səlləm şəfəq sökülənədək', 'Günəş doğmadan yola düşdü'),
     ('ثم اضطجع رسول الله', 'قبل ان تطلع الشمس'), 3),
    ('muzdalifah', 878, ('Həqiqətən də, müşriklər Günəş doğmadan ifada etmirdilər', 'ifada (Müzdəlifəni tərk) etdi'),
     ('ان المشركين كانوا لا يفيضون', 'قبل طلوع الشمس'), 4),
    ('muzdalifah', 874, ('Sevda iri cüssəli', 'ona izn verdi'), ('كانت سوده', 'فاذن لها'), 5),
    ('muzdalifah', 876, ('Rəsulullah sallallahu aleyhi və səlləm məni əhlinin arasında zəiflərin içində', 'gecə ikən göndərdi'),
     ('رسول الله', 'بليل'), 6),
    ('muzdalifah', 875, ('Ay ana, şübhəsiz ki, biz tez gəlmişik', 'qadınlara izn vermişdir'), ('اي هنتاه', 'اذن للظعن'), 7),

    # --- Cəmərat ---
    ('jamarat', 861, ('Nəhayət (Rəsulullah sallallahu aleyhi və səlləm) Muhassir vadisinin', 'hər daşı atdıqda təkbir gətirirdi'),
     ('حتى اتى بطن محسر', 'مع كل حصاه'), 0),
    ('jamarat', 880, ('Nəbi sallallahu aleyhi və səlləm Cəməratul-Aqabədə daş atana qədər təlbiyə etdi', None),
     ('ان النبي', 'جمره العقبه', 'وفي روايه'), -5),
    ('jamarat', 879, ('Biz (günortayadək) gözləyərdik', 'daşlayardıq'), ('كنا نتحين', 'رمينا'), 3),

    # --- Qurban (hədy) — əvvəl boş idi ---
    ('hady', 861, ('Sonra mənhərə (qurban kəsilən yer) gedib', 'şorbasından içdilər'), ('ثم انصرف الى المنحر', 'من مرقها'), 0),
    ('hady', 822, ('Nəhr günü bizə inək əti gətirdilər', 'zövcələrinin əvəzinə qurban kəsib'), ('فدخل علينا يوم النحر', 'عن ازواجه'), 1),
    ('hady', 894, ('Onu ayağa qaldır və Muhəmməd', 'bağlanmış halda kəs'), ('ابعثها قياما', 'مقيده'), 2),
    ('hady', 897, ('Rəsulullah sallallahu aleyhi və səlləm mənə Onun qurbanlıq dəvələrinə baxmağı', 'verməməyi əmr etdi'),
     ('امرني رسول الله', 'منها شيئا'), 3),
    ('hady', 896, ('Biz Rəsulullah sallallahu aleyhi və səlləmin zamanında qurbanlıqların ətindən', 'azuqə olaraq götürürdük'),
     ('كنا نتزود', 'الى المدينه'), 4),
    ('hady', 858, ('Sizdən kim də qurbanlıq heyvan gətirməyibsə', 'oruc tutsun'), ('ومن لم يكن منكم اهدى', 'اذا رجع الى اهله'), 5),

    # --- Saç ---
    ('shave', 883, ('Rəsulullah sallallahu aleyhi və səlləm Cəməratda daş atıb qurbanını', 'bölüşdürməsini əmr etdi'),
     ('ان رسول الله', 'بين الناس'), 0),

    # --- İfadə tavafı ---
    ('ifada', 899, ('Nəbi sallallahu aleyhi və səlləm (Hacıların qurban) kəsməsi', 'haqqında soruşuldu'), ('سئل عن الذبح', 'والتاخير'), 0),
    ('ifada', 858, ('sonra da Həccini bitirib, Nəhr', 'hər bir şeydə ihramdan çıxdı'), ('ثم لم يحلل من شيء', 'من كل شيء حرم منه'), 4),

    # --- Zəmzəm (yeni) ---
    ('zamzam', 909, ('Rəsulullah sallallahu aleyhi və səlləm içmək (üçün su) istədi', 'ayaq üstə içdi'), ('ان رسول الله', 'وهو قائم'), 0),
    ('zamzam', 861, ('(Ey) Abdul-Muttalib oğulları! (Su) çıxarın!', 'su) çıxarardım'), ('انزعوا بني عبد المطلب', 'لنزعت معكم'), 10),

    # --- Təşriq günləri ---
    ('tashriq', 915, ('Ye, (çünki,) həqiqətən də, bu günlər', 'yemək yeməyi əmr etdiyi günlərdir'), ('كل،فهذه الايام', 'بفطرها'), 2),
    ('tashriq', 910, ('Abbas ibn Abdul-Muttalib Mina gecələrində', 'su verdiyindən dolayı izn verdi'), ('ان العباس', 'من اجل سقايته'), 3),

    # --- Vida tavafı ---
    ('wada', 911, ('Nəbi sallallahu aleyhi və səlləm Zöhr, Əsr, Məğrib və İşa', '(vida) təvaf(ını) etdi'), ('ان النبي', 'فطاف به'), 3),
    ('wada', 913, ('Muhassab(da gecələmək) sünnət deyildir', 'qonaqladığı mənzildir'), ('المحصب ليست بسنه', 'لخروجه'), 4),

    # --- Ümrə ---
    ('umrah', 926, ('Bir dəfə Həcc edib, dörd dəfə də Ümrə edib', 'Həcci ilə birlikdə olan ümrəsi'), ('حجه واحده', 'مع حجته'), 3),
    ('umrah', 929, ('Həccində nə edirdinsə, Ümrəndə də onu et!', None), ('ما كنت صانعا في حجك', 'في عمرتك'), 4),
    ('umrah', 832, ('O, Nəbi sallallahu aleyhi və səlləm Ümrədə ikən', 'qısaltmışdı'), ('انه قصر', 'على المروه'), 5),
]

# Mövcud sətirlərin düzəlişi: id → (№, az sahəsi, az spec, ərəbcə lövbərlər, oxunuş spec | None)
UPDATES = {
    # № 882: ibn Ömərin «Nəbinin belə etdiyini gördüm» sözündən əvvəl kəsilmişdi — əməlin Peyğəmbərə aid olduğu
    # kartda görünmürdü.
    16: (882, 'text_az', ('O, (ibn Ömər) Cəməratud-Dunyəyə', 'belə etdiyini gördüm'),
         ('انه كان يرمي الجمره الدنيا', 'يفعله'), None),
    # № 861, Səfa zikri: ərəbcə «صدق عبده … وغلب الاحزاب» rəvayətindən (ikinci «وحده» yox) götürülmüşdü, tərcümə
    # (kitabın 2-ci qeydi) və oxunuş isə «وحده، انجز وعده، ونصر عبده، وهزم الاحزاب وحده» rəvayətinindir.
    32: (861, 'note', ('Allahdan başqa (heç bir) ilah yoxdur, O, təkdir, Onun', 'təkbaşına məğlub etdi'),
         ('لا اله الا الله', 'وهزم الاحزاب وحده', 'بدا بالصفا فرقي،ووحد الله'),
         ('Ləə iləəhə illəllahu, vahdəhu ləə şəriikə ləhu. Ləhul-mulku', 'va həzəməl-əhzəəbə vahdəhu', 'Allahı tövhid etdi, Ona təkbir etdi')),
}


def lit(s):
    return "'" + s.replace("'", "''") + "'" if s is not None else 'null'


def cut_az(text, start, end, after=None):
    base = text.find(after) if after else 0
    assert base >= 0, f'işarə tapılmadı: {after}'
    i = text.find(start, base)
    if i < 0:
        raise KeyError(f'az başlanğıc tapılmadı: {start}')
    if end is None:
        return text[i:i + len(start)]
    j = text.find(end, i)
    if j < 0:
        raise KeyError(f'az son tapılmadı: {end}')
    return text[i:j + len(end)]


def cut_ar(raw, anchors):
    if len(anchors) == 3:  # (başlanğıc, son, işarə): eyni hədisin sonrakı rəvayətində axtar
        f, idx = folded_with_map(raw)
        k = f.find(''.join(fold(c) for c in anchors[2]))
        if k < 0:
            raise KeyError(f'ar işarə tapılmadı: {anchors[2]}')
        sub, anchors = raw[idx[k]:], anchors[:2]
    else:
        sub = raw
    ar = cut(sub, *anchors).strip('{}"«» ')
    k = raw.find(ar)
    if ar.count('{') > ar.count('}') and raw[k + len(ar):k + len(ar) + 1] == '}':
        ar += '}'
    return ar


rows, checks, errors = [], [], []
for topic, no, az_spec, anchors, sort_no in ROWS:
    h = H[no]
    try:
        az = cut_az(h['text_az'], *az_spec)
        ar = cut_ar(h['text_ar'], anchors)
    except (KeyError, AssertionError) as e:
        errors.append(f'{topic}/{sort_no} №{no}: {e}')
        continue
    for label, s in (('az', az), ('ar', ar)):
        if '{{' in s or '}}' in s:
            errors.append(f'{topic}/{sort_no} №{no}: {label} çıxarışında {{{{ }}}} var')
    if len(ar) > 3 * max(len(az), 40):
        errors.append(f'{topic}/{sort_no} №{no}: ərəbcə çox uzundur ({len(ar)} / az {len(az)})')
    a = h['text_ar'].find(ar)
    b = h['text_az'].find(az)
    assert a >= 0 and b >= 0
    rows.append(f"({lit(topic)}, {h['id']}, {a + 1}, {len(ar)}, {b + 1}, {len(az)}, "
                f"{lit('Muheymin 2-ci cild, № ' + str(no))}, {sort_no})")
    checks.append((topic, sort_no, hashlib.md5(ar.encode()).hexdigest(), hashlib.md5(az.encode()).hexdigest()))
    if PRINT:
        print(f'\n### {topic} / {sort_no} — № {no}\nAZ: {az}\nAR: {ar}')

updates = []
for eid, (no, field, az_spec, anchors, tr_spec) in UPDATES.items():
    h = H[no]
    try:
        az = cut_az(h[field], *az_spec)
        ar = cut_ar(h['text_ar'], anchors)
        tr = cut_az(h['text_az'], *tr_spec) if tr_spec else None
    except (KeyError, AssertionError) as e:
        errors.append(f'update {eid} №{no}: {e}')
        continue
    a, b = h['text_ar'].find(ar), h[field].find(az)
    sets = [f"text_ar = substr(h.text_ar, {a + 1}, {len(ar)})", f"text_az = substr(h.{field}, {b + 1}, {len(az)})"]
    if tr:
        sets.append(f"transliteration = substr(h.text_az, {h['text_az'].find(tr) + 1}, {len(tr)})")
    updates.append((eid, h['id'], sets, hashlib.md5(ar.encode()).hexdigest(), hashlib.md5(az.encode()).hexdigest()))
    if PRINT:
        print(f'\n### UPDATE id {eid} — № {no}\nAZ: {az}\nAR: {ar}' + (f'\nTR: {tr}' if tr else ''))

if errors:
    print('\n'.join(errors))
    sys.exit(1)

open(f'{OUT}/hajj_update.sql', 'w').write(''.join(
    f"update public.hajj_evidence e set {', '.join(sets)} from public.hadith h where h.id = {hid} and e.id = {eid};\n"
    for eid, hid, sets, _, _ in updates))
open(f'{OUT}/hajj_update_verify.sql', 'w').write(
    "select e.id, md5(e.text_ar) = v.ar as ar_ok, md5(e.text_az) = v.az as az_ok from (values "
    + ','.join(f"({eid},'{a}','{z}')" for eid, _, _, a, z in updates)
    + ") v(id, ar, az) join public.hajj_evidence e on e.id = v.id")

sql = ("insert into public.hajj_evidence (topic, kind, source_type, hadith_id, text_ar, text_az, source, sort_no)\n"
       "select v.topic, 'evidence', 'hadith', h.id, substr(h.text_ar, v.ar_s, v.ar_n), substr(h.text_az, v.az_s, v.az_n),\n"
       "       v.src, v.sort_no\n"
       "from (values\n" + ',\n'.join(rows) + "\n) v(topic, hid, ar_s, ar_n, az_s, az_n, src, sort_no)\n"
       "join public.hadith h on h.id = v.hid\non conflict do nothing;\n")
open(f'{OUT}/hajj_seed.sql', 'w').write(sql)
vals = ','.join(f"('{k}',{s},'{a}','{z}')" for k, s, a, z in checks)
open(f'{OUT}/hajj_verify.sql', 'w').write(
    "select count(*) as expected, count(e.id) as found, count(*) filter (where md5(e.text_ar) = v.ar) as ar_ok, "
    "count(*) filter (where md5(e.text_az) = v.az) as az_ok, string_agg(case when e.id is null or md5(e.text_ar) <> v.ar "
    "or md5(e.text_az) <> v.az then v.topic || '/' || v.s end, ', ') as bad from (values " + vals + ") v(topic, s, ar, az) "
    "left join public.hajj_evidence e on e.topic = v.topic and e.sort_no = v.s")
print(f'{len(rows)} sətir')
