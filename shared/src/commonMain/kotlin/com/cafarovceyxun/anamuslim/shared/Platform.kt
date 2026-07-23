package com.cafarovceyxun.anamuslim.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
