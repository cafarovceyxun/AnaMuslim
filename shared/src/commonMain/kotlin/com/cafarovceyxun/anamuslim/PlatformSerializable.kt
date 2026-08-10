package com.cafarovceyxun.anamuslim

/**
 * Multiplatform marker for classes that are `java.io.Serializable` on Android
 * (needed for Bundle/Intent passing) and a no-op marker on iOS.
 */
expect interface PlatformSerializable
