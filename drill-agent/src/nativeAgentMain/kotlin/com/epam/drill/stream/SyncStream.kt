package com.epam.drill.stream

import com.epam.drill.ByteArrayBuilder
import com.epam.drill.lang.alloc2
import com.epam.drill.lang.smallBytesPool
import com.epam.drill.lang.invalidOp
import com.epam.drill.lang.unsupported
import kotlin.math.max
import kotlin.math.min
interface Closeable {
    fun close()
}
interface SyncInputStream : Closeable {
    fun read(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int
    fun read(): Int = smallBytesPool.alloc2 { if (read(it, 0, 1) > 0) it[0].unsigned else -1 }
}

interface SyncOutputStream : Closeable {
    fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
    fun write(byte: Int) = smallBytesPool.alloc2 { it[0] = byte.toByte(); write(it, 0, 1) }
    fun flush() = Unit
}

interface SyncPositionStream {
    var position: Long
}

interface SyncLengthStream {
    var length: Long
}

interface SyncRAInputStream {
    fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface SyncRAOutputStream {
    fun write(position: Long, buffer: ByteArray, offset: Int, len: Int)
    fun flush(): Unit = Unit
}

open class SyncStreamBase : Closeable, SyncRAInputStream,
    SyncRAOutputStream, SyncLengthStream {
    val smallTemp = ByteArray(16)
    override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = unsupported()
    override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit =
        unsupported()

    @Suppress("UNUSED_PARAMETER")
    override var length: Long set(value) = unsupported(); get() = unsupported()
    override fun close() = Unit
}

interface Extra {
    var extra: LinkedHashMap<String, Any?>?

    class Mixin(override var extra: LinkedHashMap<String, Any?>? = null) : Extra

}

class SyncStream(val base: SyncStreamBase, override var position: Long = 0L) : Extra by Extra.Mixin(),
    Closeable,
    SyncInputStream, SyncPositionStream,
    SyncOutputStream, SyncLengthStream {
    private val smallTemp = base.smallTemp

    override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        val read = base.read(position, buffer, offset, len)
        position += read
        return read
    }

    override fun read(): Int {
        val size = read(smallTemp, 0, 1)
        if (size <= 0) return -1
        return smallTemp[0].unsigned
    }

    override fun write(buffer: ByteArray, offset: Int, len: Int) {
        base.write(position, buffer, offset, len)
        position += len
    }

    override fun write(byte: Int) {
        smallTemp[0] = byte.toByte()
        write(smallTemp, 0, 1)
    }

    override var length: Long
        set(value) = run { base.length = value }
        get() = base.length

    override fun flush() {
        base.flush()
    }

    override fun close(): Unit = base.close()


    override fun toString(): String = "SyncStream($base, $position)"
}


fun MemorySyncStream(data: ByteArrayBuilder) = MemorySyncStreamBase(data).toSyncStream()
inline fun MemorySyncStreamToByteArray(initialCapacity: Int = 4096, callback: SyncStream.() -> Unit): ByteArray {
    val buffer = ByteArrayBuilder(initialCapacity)
    val s = MemorySyncStream(buffer)
    callback(s)
    return buffer.toByteArray()
}


class MemorySyncStreamBase(var data: ByteArrayBuilder) : SyncStreamBase() {

    var ilength: Int
        get() = data.size
        set(value) = run { data.size = value }

    override var length: Long
        get() = data.size.toLong()
        set(value) = run { data.size = value.toInt() }

    fun checkPosition(position: Long) = run { if (position < 0) invalidOp("Invalid position $position") }

    override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
        checkPosition(position)
        val ipos = position.toInt()
        //if (position !in 0 until ilength) return -1
        if (position !in 0 until ilength) return 0
        val end = min(this.ilength, ipos + len)
        val actualLen = max((end - ipos), 0)
        arraycopy(this.data.data, ipos, buffer, offset, actualLen)
        return actualLen
    }

    override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
        checkPosition(position)
        data.size = max(data.size, (position + len).toInt())
        arraycopy(buffer, offset, this.data.data, position.toInt(), len)
    }


    fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit =
        run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }


    override fun close() = Unit

    override fun toString(): String = "MemorySyncStreamBase(${data.size})"
}


fun SyncOutputStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)

fun SyncOutputStream.write8(v: Int): Unit = write(v)


fun SyncOutputStream.write16BE(v: Int): Unit = run { write8((v ushr 8) and 0xFF); write8(v and 0xFF) }
fun SyncOutputStream.write32BE(v: Int): Unit =
    run { write8((v ushr 24) and 0xFF); write8((v ushr 16) and 0xFF); write8((v ushr 8) and 0xFF); write8(v and 0xFF) }

fun SyncStreamBase.toSyncStream(position: Long = 0L) =
    SyncStream(this, position)


