package com.cafarovceyxun.anamuslim.utils.app

/**
 * Stable identifier for the store this build ships through: `"android"` or `"ios"`.
 *
 * Distinct from [com.cafarovceyxun.anamuslim.shared.getPlatform], which returns a human-readable
 * name ("iOS 26.5") — this one is a key, and it is written into queries and rows, so it must not
 * drift with an OS version string.
 */
expect val appPlatformId: String
