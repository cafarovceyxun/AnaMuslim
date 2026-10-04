"""Cuts the Arabic counterpart of every Phase 1 excerpt straight out of the stored `hadith.text_ar`.

Anchors are written without harakat; matching ignores harakat, tatweel, hamza seats and
presentation forms, but the returned slice is the *raw* stored text, so the reader's
highlight (which searches the stored text) lands exactly.
"""
import json, re, sys, unicodedata

S = '/private/tmp/claude-501/-Users-macbook-Desktop-AnaMuslim/aa615236-1c22-4cdd-8c1d-03fdc4b14fcc/scratchpad'
exec(open(f'{S}/phase1_data.py').read().split('# --- Excerpts shown')[0])

HARAKA = re.compile('[ً-ْٰـ­]')


def fold(ch):
    c = unicodedata.normalize('NFKC', ch)
    c = HARAKA.sub('', c)
    c = re.sub('[أإآٱ]', 'ا', c).replace('ى', 'ي').replace('ة', 'ه')
    c = re.sub(r'\s+', ' ', c)
    return c


def folded_with_map(raw):
    out, idx = [], []
    for i, ch in enumerate(raw):
        f = fold(ch)
        for c in f:
            if c == ' ' and out and out[-1] == ' ':
                continue
            out.append(c)
            idx.append(i)
    return ''.join(out), idx


def fold_anchor(a):
    return ''.join(fold(c) for c in a)


def cut(key, start, end=None):
    raw = BY[key]['text_ar']
    f, idx = folded_with_map(raw)
    fs = fold_anchor(start)
    i = f.find(fs)
    if i < 0:
        raise KeyError(f'start not found {key}: {start}')
    fe = fold_anchor(end or start)
    j = f.find(fe, i) if end else i
    if j < 0:
        raise KeyError(f'end not found {key}: {end}')
    j_end = j + len(fe) - 1
    a, b = idx[i], idx[j_end] + 1
    # keep trailing harakat of the last letter
    while b < len(raw) and HARAKA.match(raw[b]):
        b += 1
    return raw[a:b].strip()


AR = {
    'w1_mouth': ('114', 'فغسل وجهه', 'واستنشق'),
    'w1_face': ('114', 'ثم أخذ غرفة من ماء،فجعل بها هكذا', 'فغسل بهما وجهه'),
    'w1_rarm': ('114', 'فغسل بها يده اليمنى'),
    'w1_larm': ('114', 'فغسل بها يده اليسرى'),
    'w1_head': ('114', 'ثم مسح برأسه'),
    'w1_rfoot': ('114', 'فرش على رجله اليمنى', 'حتى غسلها'),
    'w1_lfoot': ('114', 'ثم أخذ غرفة أخرى', 'يعني اليسرى'),
    'w1_end': ('114', 'هكذا رأيت رسول الله', 'يتوضأ'),
    'w1_113': ('113', 'فتوضأ مرة مرة'),
    'w3_hands': ('112', 'فغسل يده مرتين'),
    'w3_mouth': ('112', 'ثم مضمض واستنثر ثلاثا'),
    'w3_face': ('112', 'ثم غسل وجهه ثلاثا'),
    'w3_arms': ('112', 'ثم غسل يديه مرتين مرتين', 'إلى المرفقين'),
    'w3_head': ('112', 'بدأ بمقدم رأسه', 'الذي بدأ منه'),
    'w3_feet': ('112', 'ثم غسل رجليه'),
    'w_sleep': ('125', 'فليغسل يده', 'في وضوئه'),
    'w_nose': ('115', 'فليجعل في أنفه ماء', 'ثم لينثر'),
    'w_right': ('109', 'يحب التيمن ما استطاع'),
    'w_heels': ('111', 'ويل للعقب من النار'),
    'w_mudd': ('110', 'وكان يتوضأ بالمكوك'),
    'w_many': ('118', 'يجزئ أحدنا الوضوء', 'ما لم يحدث'),
    'w_doubt': ('119', 'إنه لا يجب عليه شيء', 'يسمع صوته'),
    'w_madhy': ('137', 'إذا رأيته فتوضأ واغسله'),
    'w_meat': ('128', 'ثم قام فصلى', 'ولم يتوضأ'),
    'w_milk': ('126', 'إن له دسما'),
    'w_eat': ('98', 'ثم أتي بطعام', 'ولم يمس ماء'),
    'w_miswak': ('122', 'لأمرتهم بالسواك مع الوضوء'),
    'g_hands': ('140', 'بدأ فغسل يده', 'في الإناء'),
    'g_private': ('140', 'ثم يغسل فرجه'),
    'g_wudu': ('140', 'ثم يتوضأ وضوءه للصلاة'),
    'g_hair': ('140', 'ثم يشرب شعره الماء'),
    'g_three': ('140', 'ثم يحثي على رأسه', 'ثلاث حثيات'),
    'g_sides': ('141', 'فبدأ بشق رأسه الأيمن', 'على رأسه'),
    'g_body': ('145', 'أن تحثي على رأسك', 'فتطهرين'),
    'g_braids': ('145', 'أفأنفضه لغسل الجنابة'),
    'g_wet': ('146', 'نعم،إذا رأت الماء'),
    'g_noinzal1': ('134', 'يغسل ما مس المرأة منه', 'ويصلي'),
    'g_noinzal2': ('135', 'إذا أعجلت أو قحطت', 'وعليك الوضوء'),
    'g_saa': ('143', 'يكفيك صاع'),
    'g_friday': ('437', 'غسل يوم الجمعة واجب', 'على كل محتلم'),
    'g_sleep': ('150', 'غسل فرجه', 'وتوضأ للصلاة'),
    't_strike': ('180', 'وضرب بكفيه الأرض'),
    't_blow': ('180', 'ثم نفخ فيهما'),
    't_wipe': ('180', 'ثم مسح بهما وجهه', 'وبعض ذراعيه'),
    't_enough': ('180', 'كان الصعيد الطيب كافيك'),
    't_junub': ('181', 'عليك بالصعيد', 'يكفيك'),
    't_wall': ('182', 'أقبل على الجدار', 'ويديه'),
    't_earth': ('177', 'جعلت لي الأرض', 'طهورا ومسجدا'),
    'k_wipe': ('131', 'فتوضأ ومسح على خفيه'),
    'k_time': ('133', 'ثلاثة أيام ولياليهن', 'للمقيم'),
    'k_last': ('132', 'رأيت رسول الله', 'صنع مثل هذا'),
    'a_dua': ('91', 'أعوذ بالله من الخبث', 'والخبائث'),
    'a_qibla': ('94', 'لا تستقبلوا القبلة', 'أو غربوا'),
    'a_house': ('95', 'مستدبر البيت', 'مستقبل الشام'),
    'a_right': ('92', 'أن يستنجي أحدنا بيمينه'),
    'a_three': ('92', 'لا يستنجي أحدكم', 'ثلاثة أحجار'),
    'a_bone': ('92', 'وينهانا عن الروث', 'والعظام'),
    'a_odd': ('115', 'ومن استجمر فليوتر'),
    'a_still': ('99', 'لا يبولن أحدكم في الماء الدائم'),
    'a_stand': ('97', 'فبال قائما'),
    'n_baby': ('105', 'فدعا رسول الله', 'فأتبعه إياه'),
    'n_mosque': ('103', 'وهريقوا على بوله', 'ذنوبا من ماء'),
    'n_ease': ('103', 'فإنما بعثتم ميسرين', 'معسرين'),
    'n_dog': ('107', 'أن يغسله سبع مرات', 'بالتراب'),
    'n_blood': ('166', 'فلتقرصه', 'ثم لتصل فيه'),
    'n_mani': ('147', 'فيغسله من ثوبه', 'إلى الصلاة'),
    'n_mani2': ('148', 'وما أزيد أن أفركه', 'من ثوب رسول الله'),
    'n_muslim': ('158', 'إن المؤمن لا ينجس'),
    'f_prayer': ('161', 'وتمكث الليالي لا تصلي', 'وتفطر في رمضان'),
    'f_qada': ('162', 'كنا نحيض', 'أفكنا نقضي'),
    'f_istihada': ('163', 'فأمرها أن تغتسل وتصلي'),
    't_nowater': ('180', 'إنا نمكث الشهر', 'لا نجد الماء'),
    'toilet_dua_tr': ('91', 'اللهم إني أعوذ بك', 'والخبائث'),
    'f_yellow': ('164', 'كنا لا نعد الكدرة والصفرة شيئا'),
    'f_ghusl': ('165', 'تتبعي بها أثر الدم'),
    'f_eid': ('174', 'والحيض يعتزلن الصلاة'),
}

if __name__ == '__main__':
    data = json.load(open(f'{S}/phase1.json'))
    missing = [k for k in data['EX'] if k not in AR]
    bad = []
    out = {}
    for k, spec in AR.items():
        try:
            out[k] = cut(*spec)
        except KeyError as e:
            bad.append(str(e))
    print('missing anchors:', missing)
    print('failures:', *bad, sep='\n  ')
    for k in ['w1_mouth', 'w3_head', 'g_sides', 't_junub', 'a_dua', 'n_mani2']:
        if k in out:
            print(k, '=>', out[k])
    json.dump(out, open(f'{S}/phase1_ar.json', 'w'), ensure_ascii=False)
    print(len(out))
