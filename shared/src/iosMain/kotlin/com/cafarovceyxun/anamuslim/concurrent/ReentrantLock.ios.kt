package com.cafarovceyxun.anamuslim.concurrent

import platform.Foundation.NSRecursiveLock

actual class ReentrantLock actual constructor() {
    private val delegate = NSRecursiveLock()
    actual fun lock() {
        delegate.lock()
    }

    actual fun unlock() {
        delegate.unlock()
    }
}
