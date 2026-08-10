package com.cafarovceyxun.anamuslim.utils.univ

/**
 * Focused printf-style formatter for iOS. Supports `%%` and `%[0][width][d|i|x|X|s]`,
 * which covers every format string used in this app. Unknown specifiers are emitted
 * verbatim so mistakes are visible rather than silently dropped.
 */
actual fun stringFormatInvariant(format: String, vararg args: Any?): String {
    val sb = StringBuilder()
    var argIndex = 0
    var i = 0
    while (i < format.length) {
        val c = format[i]
        if (c != '%') {
            sb.append(c)
            i++
            continue
        }
        i++
        if (i < format.length && format[i] == '%') {
            sb.append('%')
            i++
            continue
        }
        var zeroPad = false
        while (i < format.length && (format[i] == '0' || format[i] == '-' || format[i] == ' ' || format[i] == '+')) {
            if (format[i] == '0') zeroPad = true
            i++
        }
        var width = 0
        while (i < format.length && format[i].isDigit()) {
            width = width * 10 + (format[i] - '0')
            i++
        }
        if (i >= format.length) {
            sb.append('%')
            break
        }
        val conv = format[i]
        i++
        val arg = if (argIndex < args.size) args[argIndex++] else null
        val out = when (conv) {
            'd', 'i' -> (arg as? Number)?.toLong()?.toString() ?: arg.toString()
            'x' -> (arg as? Number)?.toLong()?.toString(16) ?: arg.toString()
            'X' -> ((arg as? Number)?.toLong()?.toString(16) ?: arg.toString()).uppercase()
            's' -> arg?.toString() ?: "null"
            else -> {
                sb.append('%').append(conv)
                continue
            }
        }
        if (width > out.length) {
            sb.append((if (zeroPad) "0" else " ").repeat(width - out.length))
        }
        sb.append(out)
    }
    return sb.toString()
}
