package com.cafarovceyxun.anamuslim.utils

/**
 * Unicode NFKC (compatibility composition) normalization.
 * Platform-backed: Android uses [java.text.Normalizer], iOS uses
 * `NSString.precomposedStringWithCompatibilityMapping`.
 */
expect fun nfkcNormalize(input: String): String
