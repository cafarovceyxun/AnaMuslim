package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * İnterfeys mətnlərinin ümumi ölçü çarpanı — bir sürüşdürücü ilə bütün tətbiqi böyüdüb-kiçildir.
 *
 * **Quran və hədis mətnlərinə toxunmur.** Onların öz çarpanları var
 * ([com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences] və
 * [com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences]) və oxucu mətnləri
 * ölçüsünü həmişə **açıq** yazır (`typography.bodyLarge.copy(fontSize = 16.sp * mult)`), yəni
 * miqyas [getAppTypography]-nin dəyərini üstələdiyi yerə heç düşmür. İki ölçü bir-birinə vurulsaydı
 * oxucunun öz faizi yalan danışardı: «150%» yazan sürüşdürücü ekranda 225% göstərərdi.
 *
 * Faiz kimi saxlanır (piksel yox): sürüşdürücüdə göstərilən ədəd elə saxlanan ədədin özüdür və
 * [com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep] ilə eyni dildə danışır.
 */
object AppTextScale {

    const val MIN_PERCENT = 80
    const val MAX_PERCENT = 150
    const val DEFAULT_PERCENT = 100

    /** Sürüşdürücü 5%-lik pillələrlə hərəkət edir — daha xırda fərq gözlə seçilmir. */
    const val PERCENT_STEP = 5

    /** Faizi mətn ölçüsünə vurulan çarpana çevirir. */
    fun factor(percent: Int): Float =
        percent.coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f
}

/**
 * Cari interfeys mətn çarpanı.
 *
 * [getAppTypography] tipoqrafiyanın özünü artıq miqyaslayır, ona görə `MaterialTheme.typography.X`
 * işlədən hər yer öz-özünə böyüyür. Bu lokal yalnız ölçünü **əl ilə** yazan bir neçə interfeys
 * etiketi üçündür (`fontSize = 10.sp * LocalAppTextScale.current`) — onlar olmasa sürüşdürücüdən
 * sonra ekranda bir neçə yazı köhnə ölçüdə ilişib qalır.
 *
 * Mövzudan kənarda (vidcet önizləmələri, testlər) `1f` qalır.
 */
val LocalAppTextScale = staticCompositionLocalOf { 1f }
