package com.cafarovceyxun.anamuslim.compose.utils

import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * `UNUserNotificationCenter.delegate` **tək qlobaldır** — ikinci modul onu qurarsa birincinin
 * toxunuşları səssizcə ölür.
 *
 * Əvvəl bu delegate `IosDailyReminder`-in içində idi; namaz bildirişləri əlavə olunanda ora ikinci
 * sahib gəldi, ona görə delegate buraya, ortaq registry-yə çıxarıldı. Hər modul öz `kind`-ını
 * qeydiyyatdan keçirir, toxunuş isə `userInfo["kind"]`-a görə yönləndirilir.
 *
 * ⚠️ **Miqrasiya yolu məcburidir.** Yeniləmədən əvvəl yazılmış gözləyən tələblərdə `kind` açarı
 * **yoxdur** (VOTD onları `votd_slot_…` identifikatoru ilə qurmuşdu). Prefiks ehtiyat yolu olmasa
 * yeniləmədən sonra bir-iki gün ərzində günün ayəsi toxunuşları ölərdi və bu, nə kompilyasiyada,
 * nə testdə görünərdi.
 */
object IosNotificationCenterDelegate {

    /** `userInfo`-da modulu bildirən açar. Yeni tələblər onu həmişə yazır. */
    const val KEY_KIND = "kind"

    private class Handler(
        val kind: String,
        val identifierPrefix: String,
        val onTap: (Map<Any?, *>) -> Unit,
    )

    private val handlers = mutableListOf<Handler>()
    private var installed = false

    /**
     * [kind] modulun adı, [identifierPrefix] onun tələb identifikatorlarının prefiksi (köhnə,
     * `kind`-siz tələbləri tanımaq üçün).
     */
    fun register(kind: String, identifierPrefix: String, onTap: (Map<Any?, *>) -> Unit) {
        handlers.removeAll { it.kind == kind }
        handlers += Handler(kind, identifierPrefix, onTap)
    }

    /** İdempotent — delegate yalnız bir dəfə qurulur. */
    fun install() {
        if (installed) return
        installed = true
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
    }

    private val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit,
        ) {
            val request = didReceiveNotificationResponse.notification.request
            val info = request.content.userInfo

            val kind = (info[KEY_KIND] as? String)
                ?: handlers.firstOrNull { request.identifier.startsWith(it.identifierPrefix) }?.kind

            // Tanınmayan növ: tətbiq onsuz da açılır, əlavə naviqasiya edilmir.
            handlers.firstOrNull { it.kind == kind }?.onTap?.invoke(info)

            withCompletionHandler()
        }

        // Bu olmasa bildiriş tətbiq ön plandaykən udulur.
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
        ) {
            withCompletionHandler(
                UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound
            )
        }
    }
}
