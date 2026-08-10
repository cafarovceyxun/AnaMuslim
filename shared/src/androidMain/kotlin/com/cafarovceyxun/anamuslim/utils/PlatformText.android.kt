package com.cafarovceyxun.anamuslim.utils

import java.text.Normalizer

actual fun nfkcNormalize(input: String): String =
    Normalizer.normalize(input, Normalizer.Form.NFKC)
