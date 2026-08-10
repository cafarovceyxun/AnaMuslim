/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/4/2022.
 * All rights reserved.
 */
package com.cafarovceyxun.anamuslim.utils.exceptions

import kotlin.jvm.JvmOverloads

class NoInternetException @JvmOverloads constructor(msg: String? = "No internet") : RuntimeException(msg)
