/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/4/2022.
 * All rights reserved.
 */
package com.cafarovceyxun.anamuslim.utils.exceptions

import kotlin.jvm.JvmOverloads

class HttpNotFoundException @JvmOverloads constructor(msg: String? = "Not found") : RuntimeException(msg)
