package com.cafarovceyxun.anamuslim.utils.app

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.core.net.toUri
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound

/**
 * [AdhanSound] → `res/raw` id-si. **Statik** xəritədir və məhz buna görə lazımdır.
 *
 * ⚠️ Faylı adı ilə axtarmaq (`Resources.getIdentifier`) və ya adla URI qurmaq
 * (`android.resource://<pkg>/raw/prayer_call`) release-i **səssizcə** sındırır: release-də
 * `isShrinkResources = true`, R8-in resurs təmizləyicisi isə heç bir `R.raw.*` müraciəti görmür
 * və altı səsi də «not reachable» sayıb `resources.arsc`-dən silir. 2026-09-16-da ölçüldü —
 * `2026.09.15-release.apk`-ın içində `res/raw` qovluğu ümumiyyətlə yox idi (debug APK-da altı
 * `.m4a` da yerində), `app/build/outputs/mapping/release/resources.txt` isə
 * `raw:prayer_call:2131820545 is not reachable.` yazırdı. Nəticə istifadəçidə belə görünür:
 * önizləmə düyməsi heç nə çalmır (`getIdentifier` 0 qaytarır), namaz bildirişi isə adi sistem
 * səsi ilə gəlir (kanalın səsi mövcud olmayan resursa işarə edir). Nə kompilyator, nə testlər,
 * nə də debug build bunu göstərir — yalnız release APK-nı açıb baxmaq göstərir.
 *
 * `when` **tam** olmalıdır (`else` yazma): yeni səs əlavə edəndə kompilyator buranı da göstərsin.
 */
@RawRes
fun AdhanSound.rawResIdOrNull(): Int? = when (this) {
    AdhanSound.CALL -> R.raw.prayer_call
    AdhanSound.VOICE_AR -> R.raw.prayer_voice_ar
    AdhanSound.RINGTONE -> R.raw.prayer_ringtone
    AdhanSound.PHONE -> R.raw.prayer_phone
    AdhanSound.VIBRATING -> R.raw.prayer_vibrating
    // Cihazın öz səsi / səssiz — öz faylı yoxdur.
    AdhanSound.SYSTEM_DEFAULT, AdhanSound.SILENT -> null
}

/**
 * Səsin `android.resource://` URI-si — **nömrə ilə**, adla yox.
 *
 * Ad formasından (`…/raw/prayer_call`) qəsdən imtina olunub: o, resurs cədvəlindəki adların
 * qalmasından asılıdır, nömrə forması isə release-də ad qısaldılsa da işləyir.
 */
fun AdhanSound.notificationSoundUri(ctx: Context): Uri? =
    rawResIdOrNull()?.let { "android.resource://${ctx.packageName}/$it".toUri() }
