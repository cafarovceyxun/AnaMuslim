package com.cafarovceyxun.anamuslim.concurrent

/**
 * A minimal reentrant lock for serializing access to a shared resource across threads.
 *
 * androidMain aliases `java.util.concurrent.locks.ReentrantLock`; iosMain wraps `NSRecursiveLock`.
 * Both are reentrant, so nested [withLock] calls on the same thread are safe.
 */
expect class ReentrantLock() {
    fun lock()
    fun unlock()
}

/** Runs [block] while holding the lock, releasing it afterwards even on failure. */
fun <T> ReentrantLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
