package com.cafarovceyxun.anamuslim.components

import com.cafarovceyxun.anamuslim.PlatformSerializable
import kotlin.jvm.Transient

open class ComponentBase : PlatformSerializable {
    var id = -1
    var key: String? = null
    var position = -1
    var selected = false
    var enabled = true

    @Transient
    var obj: Any? = null
}
