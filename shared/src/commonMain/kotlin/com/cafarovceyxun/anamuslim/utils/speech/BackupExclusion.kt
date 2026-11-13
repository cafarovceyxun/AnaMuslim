package com.cafarovceyxun.anamuslim.utils.speech

/**
 * [path]-i (qovluq və ya fayl) sistemin ehtiyat nüsxəsindən çıxarır.
 *
 * iOS-da tətbiq faylları `Documents`-dədir və iCloud-a yedəklənir — yenidən endirilə bilən 64 MB-lıq
 * səs modeli orada qalsa həm istifadəçinin iCloud yerini yeyir, həm də App Store qaydasına (2.23)
 * ziddir. Android-də heç nə etmir: `backup_rules.xml` yalnız sadalanan qovluqları götürür.
 */
internal expect fun excludeFromBackup(path: String)
