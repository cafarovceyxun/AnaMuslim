package com.cafarovceyxun.anamuslim.utils

import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping

actual fun nfkcNormalize(input: String): String =
    (input as NSString).precomposedStringWithCompatibilityMapping
