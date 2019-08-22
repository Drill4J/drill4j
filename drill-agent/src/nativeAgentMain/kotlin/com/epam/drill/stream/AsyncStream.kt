@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.epam.drill.stream

import com.epam.drill.ByteArrayBuilder
import com.epam.drill.lang.alloc2
import com.epam.drill.lang.allocThis
import com.epam.drill.lang.smallBytesPool
import com.epam.drill.lang.Charset
import com.epam.drill.lang.EOFException
import com.epam.drill.lang.UTF8
import com.epam.drill.lang.toString
import com.epam.drill.readS32BE
import com.epam.drill.readU16BE
inline val Byte.unsigned get() = this.toInt() and 0xFF

interface AsyncCloseable {
    suspend fun close()
}

interface AsyncBaseStream : AsyncCloseable

interface AsyncInputStream : AsyncBaseStream {
    suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
    suspend fun read(): Int = smallBytesPool.alloc2 { if (read(it, 0, 1) > 0) it[0].unsigned else -1 }
}

interface AsyncOutputStream : AsyncBaseStream {
    suspend fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
    suspend fun write(byte: Int) = smallBytesPool.alloc2 { it[0] = byte.toByte(); write(it, 0, 1) }
}

interface AsyncGetPositionStream : AsyncBaseStream {
    suspend fun getPosition(): Long = throw UnsupportedOperationException()
}

interface AsyncGetLengthStream : AsyncBaseStream {
    suspend fun getLength(): Long = throw UnsupportedOperationException()
}


interface AsyncInputStreamWithLength : AsyncInputStream,
    AsyncGetPositionStream, AsyncGetLengthStream

fun List<AsyncInputStreamWithLength>.combine(): AsyncInputStreamWithLength {
    val list = this
    return object : AsyncInputStreamWithLength {
        override suspend fun getPosition(): Long = list.map { it.getPosition() }.sum()
        override suspend fun getLength(): Long = list.map { it.getLength() }.sum()

        override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
            list.fastForEach { i ->
                val read = i.read(buffer, offset, len)
                if (read > 0) return read
            }
            return -1
        }

        override suspend fun close() {
            list.fastForEach { i ->
                i.close()
            }
        }
    }
}

operator fun AsyncInputStreamWithLength.plus(other: AsyncInputStreamWithLength): AsyncInputStreamWithLength =
    listOf(this, other).combine()

suspend fun AsyncInputStream.readExact(buffer: ByteArray, offset: Int, len: Int) {
    var remaining = len
    var coffset = offset
    val reader = this
    while (remaining > 0) {
        val read = reader.read(buffer, coffset, remaining)
        if (read < 0) break
        if (read == 0) throw EOFException("Not enough data. Expected=$len, Read=${len - remaining}, Remaining=$remaining")
        coffset += read
        remaining -= read
    }
}



internal suspend inline fun <R> AsyncInputStream.readSmallTempExact(size: Int, callback: ByteArray.() -> R): R =
    smallBytesPool.allocThis {
        val read = read(this, 0, size)
        if (read != size) error("Couldn't read exact size=$size but read=$read")
        callback()
    }


suspend fun AsyncInputStream.readBytesExact(len: Int): ByteArray = ByteArray(len).apply { readExact(this, 0, len) }
suspend fun AsyncInputStream.readU8(): Int = read()
suspend fun AsyncInputStream.readU16BE(): Int = readSmallTempExact(2) { readU16BE(0) }
suspend fun AsyncInputStream.readS32BE(): Int = readSmallTempExact(4) { readS32BE(0) }

suspend fun AsyncOutputStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)

suspend fun AsyncInputStream.readLine(eol: Char = '\n', charset: Charset = UTF8): String {
    val temp = ByteArray(1)
    val out = ByteArrayBuilder()
    try {
        while (true) {
            val c = run { readExact(temp, 0, 1); temp[0] }

            if (c.toChar() == eol) break
            out.append(c)
        }
    } catch (e: EOFException) {
    }
    return out.toByteArray().toString(charset)
}

inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this[n++])
    }
}
