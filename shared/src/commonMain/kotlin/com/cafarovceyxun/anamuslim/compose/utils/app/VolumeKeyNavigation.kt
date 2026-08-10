package com.cafarovceyxun.anamuslim.compose.utils.app

/**
 * Whether the platform can page the reader with the hardware volume keys.
 *
 * Android hosts the reader in an Activity that intercepts `KeyEvent.KEYCODE_VOLUME_UP/DOWN` and
 * feeds them to `handleScrollKey`. iOS gives apps no equivalent: the volume buttons belong to the
 * system, and the only way to observe them is a documented-as-forbidden `AVAudioSession` trick.
 *
 * A plain constant rather than a provider seam, for the same reason [openAppSettings] is one: both
 * platforms have a definite answer that is known at build time, so there is nothing to register.
 */
expect val supportsVolumeKeyNavigation: Boolean
