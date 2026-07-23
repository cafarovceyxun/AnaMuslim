package com.cafarovceyxun.anamuslim.utils.extensions

/**
 * Cast a class null safely to a specific type.
 * */
inline fun <reified T> Any?.safeCastTo(block: T.() -> Unit) {
    if (this is T) {
        block()
    }
}

/**
 * Cast a class null safely to a map.
 * */
fun Any?.safeCastToMap(block: Map<String, Any>.() -> Unit) {
    safeCastTo(block)
}

/**
 * Cast a class to specific type unchecked, result may be null.
 * */
inline fun <reified T> Any?.unsafeCastTo(): T? {
    if (this is T) {
        return this
    }
    return null
}

/**
 * Cast a class specific type unchecked, result may be null.
 * */
fun Any?.unsafeCastToMap(): Map<String, Any>? {
    return unsafeCastTo<Map<String, Any>>()
}

/**
 * Cast a class to specific type or default if null.
 * */
inline fun <reified T> Any?.castOrDefault(default: T): T {
    if (this is T) {
        return this
    }
    return default
}

fun String?.throwIfNullOrEmpty(): String {
    if (isNullOrEmpty()) {
        throw Exception()
    }
    return this
}

fun Any?.isBooleanTrue(): Boolean {
    if (this is Boolean) {
        return this
    }
    return false
}

fun Number?.throwIfNullOrNotPositive(): Number {
    if (this == null || this == 0) {
        throw Exception()
    }
    return this
}

inline fun <reified T> T?.orMinusOne(): T where T : Number {
    return this ?: (-1 as T)
}

fun String?.toIntOrMinusOne(): Int {
    return try {
        this?.toInt() ?: -1
    } catch (e: Exception) {
        -1
    }
}

fun CharSequence.isOnlyLetters() = all { it.isLetter() }

inline fun Any?.ifNull(action: () -> Unit) {
    if (this == null) {
        action()
    }
}

val IntRange.normalized: IntRange
    get() = if (first <= last) this else IntRange(last, first)

val IntRange.asPair: Pair<Int, Int>
    get() = Pair(first, last)

val IntRange.isSingleValue: Boolean
    get() = first == last

val Pair<Int, Int>.asIntRange: IntRange
    get() = IntRange(first, second)
