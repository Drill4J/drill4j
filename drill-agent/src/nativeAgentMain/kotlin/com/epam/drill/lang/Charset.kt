package com.epam.drill.lang

import com.epam.drill.ByteArrayBuilder


abstract class Charset(val name: String) {
    abstract fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int = 0, end: Int = src.length)
    abstract fun decode(out: StringBuilder, src: ByteArray, start: Int = 0, end: Int = src.size)

}

open class UTC8CharsetBase(name: String) : Charset(name) {
    private fun createByte(codePoint: Int, shift: Int): Int = codePoint shr shift and 0x3F or 0x80

    override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
        for (n in start until end) {
            val codePoint = src[n].toInt()

            if (codePoint and 0x7F.inv() == 0) { // 1-byte sequence
                out.append(codePoint.toByte())
            } else {
                when {
                    codePoint and 0x7FF.inv() == 0 -> // 2-byte sequence
                        out.append((codePoint shr 6 and 0x1F or 0xC0).toByte())
                    codePoint and 0xFFFF.inv() == 0 -> { // 3-byte sequence
                        out.append((codePoint shr 12 and 0x0F or 0xE0).toByte())
                        out.append((createByte(codePoint, 6)).toByte())
                    }
                    codePoint and -0x200000 == 0 -> { // 4-byte sequence
                        out.append((codePoint shr 18 and 0x07 or 0xF0).toByte())
                        out.append((createByte(codePoint, 12)).toByte())
                        out.append((createByte(codePoint, 6)).toByte())
                    }
                }
                out.append((codePoint and 0x3F or 0x80).toByte())
            }
        }
    }

    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
        if ((start < 0 || start > src.size) || (end < 0 || end > src.size)) error("Out of bounds")
        var i = start
        while (i < end) {
            val c = src[i].toInt() and 0xFF
            when (c shr 4) {
                in 0..7 -> {
                    // 0xxxxxxx
                    out.append(c.toChar())
                    i += 1
                }
                in 12..13 -> {
                    // 110x xxxx   10xx xxxx
                    out.append((c and 0x1F shl 6 or (src[i + 1].toInt() and 0x3F)).toChar())
                    i += 2
                }
                14 -> {
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    out.append((c and 0x0F shl 12 or (src[i + 1].toInt() and 0x3F shl 6) or (src[i + 2].toInt() and 0x3F)).toChar())
                    i += 3
                }
                else -> {
                    i += 1
                }
            }
        }
    }
}

@SharedImmutable
val UTF8: Charset = UTC8CharsetBase("UTF-8")


fun String.toByteArray(charset: Charset = UTF8): ByteArray {
    val out = ByteArrayBuilder()
    charset.encode(out, this)
    return out.toByteArray()
}

fun ByteArray.toString(charset: Charset): String {
    val out = StringBuilder()
    charset.decode(out, this)
    return out.toString()
}