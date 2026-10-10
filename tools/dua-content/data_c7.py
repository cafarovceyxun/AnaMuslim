# Üçüncü dalğa (2026-10-09): 7-ci cildin sonradan yüklənən kitabları — Cənnət (sonu), Cəhənnəmdən çəkindirmək,
# Tövbə, Cənazə (hədis id 3602–4056). 183 hədis sətri oxundu (ayə sətirləri «Qurandan dualar»-dadır).
# Ukkaşə (№ 3165) və Qureyş zadəganları (№ 3234) duaları başqa cilddən artıq bazadadır — təkrarlanmır.
C7 = [
    # --- Ölüm və son anlar: ölünün yanında (Cənazə kitabı, bab 5) ---
    dict(cat='olum-ve-son-anlar', sub=None, hid=4022, sort=30,
         ar=('اللهم اغفر لنا وله', 'عقبى حسنه'),
         tr=('Allahumməğfir lənəə va ləhu', 'uqbəə həsənətən'),
         az=('note', 'Allahummə!Bizi də, onu da məğfirət et', 'əvəzlə!'),
         note=('text_az', 'Xəstənin və ya ölünün yanına gəldikdə, xeyir söyləyin!', 'amin deyirlər')),
    # --- Sıxıntı və müsibət zamanı: Nəbinin oğlu can verən qızına göndərdiyi söz (Cənazə kitabı, bab 4) ---
    dict(cat='sixinti-ve-musibet-zamani', sub=None, hid=4020, sort=30,
         ar=('ان لله ما اخذ', 'مسمى'),
         tr=('İnnə lilləhi mə əxazə', 'biəcəlin musəmmə'),
         az=('text_az', 'Şübhəsiz ki, Allaha aiddir, aldığı da, verdiyi də', '(vaxtı) vardır'),
         note=('text_az', 'qoy səbr etsin və əcrini (Allahdan) umsun', None)),
]

# İstifadəçinin seçiminə qalan bəndlər (hədisin öz sözü, dua forması yoxdur)
C7_OPT = dict(
    istigfar_70=dict(cat='zikr-ve-duanin-fezileti', sub=None, hid=3998, sort=60,
         ar=('والله اني لاستغفر الله', 'سبعين مره'),
         tr=None,
         az=('text_az', 'Vallahi, şübhəsiz ki, həqiqətən də mən, bir gündə yetmiş', 'edirəm'),
         note=None),
    istigfar_100=dict(cat='zikr-ve-duanin-fezileti', sub=None, hid=3999, sort=70,
         ar=('انه ليغان على قلبي', 'مائه مره'),
         tr=None,
         az=('text_az', 'Həqiqətən də, şübhəsiz ki, (hərdən) mənim qəlbimə qəflət çökür', 'bağışlanma istəyirəm'),
         note=None),
    zikr_qudsi=dict(cat='zikr-ve-duanin-fezileti', sub=None, hid=4002, sort=80,
         ar=('قال الله عز وجل', 'هروله'),
         tr=None,
         az=('text_az', 'Allaha azzə və cəllə dedi', 'yüyürərək gələrəm'),
         note=None),
    telqin=dict(cat='olum-ve-son-anlar', sub=None, hid=3884, sort=40,
         ar=('اي عم قل لا اله الا الله', 'عند الله'),
         tr=None,
         az=('text_az', 'Ey əmi! "Ləə iləhə illəllah" de!', 'dəlil gətirim'),
         note=None),
)
C7 += [C7_OPT[k] for k in __import__('os').environ.get('C7_OPT', '').split(',') if k]
