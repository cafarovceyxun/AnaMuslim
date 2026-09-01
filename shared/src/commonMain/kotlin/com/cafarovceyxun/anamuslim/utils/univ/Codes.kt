package com.cafarovceyxun.anamuslim.utils.univ

object Codes {
    const val APP_UPDATE = 0x100
    const val SETTINGS_LAUNCHER_RESULT_CODE = 112
    const val SETTINGS_TRANSLATION_DOWNLOAD_CODE = 116
    const val OPEN_REFERENCE_RESULT_CODE = 113
    const val PICK_IMAGE = 0x114
    const val VOICE_INPUT_ACTIVITY = 114
    const val NOTIF_ID_VOTD = 0x0001
    const val NOTIF_ID_REC_PLAYER = 0x0002
    const val NOTIF_ID_VERSE_REMINDER = 0x0003

    /**
     * Günün ayəsi bildirişlərinin baza id-si: gündə beş yuva var, hər biri **öz** bildirişini
     * göstərir (`BASE + slot`). Ona görə baza qonşu id-lərdən uzaqdır — `NOTIF_ID_VOTD + slot`
     * pleyer və ayə xatırlatması ilə toqquşurdu.
     */
    const val NOTIF_ID_VOTD_SLOT_BASE = 0x0400

    /**
     * Namaz bildirişləri: `BASE + Prayer.ordinal` (altı ədəd).
     *
     * ⚠️ VOTD yuvalarından (0x0400…0x0404) uzaqdır. Eyni id iki funksiyada işlədilsə biri
     * digərinin bildirişini əvəz edər; `PendingIntent`-in requestCode-u da bu id-dir, ona görə
     * toqquşma `FLAG_UPDATE_CURRENT` ilə **extra-ları da** əzərdi.
     */
    const val NOTIF_ID_PRAYER_BASE = 0x0500
    const val REQ_CODE_REC_PLAYER = 0x0100
    const val REQ_CODE_LOG_IN = 0x0200
    const val REQ_CODE_PROFILE_UPDATE = 0x0210
    const val REQ_CODE_PERMISSION_STORAGE_EXTERNAL = 0x0310
}
