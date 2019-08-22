package com.epam.drill

import kotlin.math.max

class ByteArrayBuilder(var data: ByteArray, size: Int = data.size, val allowGrow: Boolean = true) {
    constructor(initialCapacity: Int = 4096) : this(ByteArray(initialCapacity), 0)

    private var _size: Int = size
    var size: Int
        get() = _size
        set(value) {
            val oldPosition = _size
            ensure(value)
            _size = value
            if (value > oldPosition) {
                arrayfill(data, 0, oldPosition, value)
            }
        }

    fun arrayfill(array: ByteArray, value: Byte, start: Int, end: Int): Unit =
        run { for (n in start until end) array[n] = value }

    private fun ensure(expected: Int) {
        if (data.size < expected) {
            if (!allowGrow) throw RuntimeException("ByteArrayBuffer configured to not grow!")
            data = data.copyOf(max(expected, (data.size + 7) * 5))
        }
    }

    private inline fun <T> prepare(count: Int, callback: () -> T): T {
        ensure(_size + count)
        return callback().also { _size += count }
    }

    fun append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) {
        prepare(len) {
            arraycopy(array, offset, this.data, _size, len)
        }
    }

    private fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit =
        run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }


    fun append(v: Byte) = this.apply { prepare(1) { data[_size] = v } }

    fun append(vararg v: Byte) = append(v)
    fun append(vararg v: Int) = this.apply {
        prepare(v.size) {
            for (n in 0 until v.size) this.data[this._size + n] = v[n].toByte()
        }
    }


    fun toByteArray(): ByteArray = data.copyOf(_size)
}

inline class ByteArrayBuilderLE(val bab: ByteArrayBuilder)

val ByteArrayBuilderLE.size get() = bab.size
fun ByteArrayBuilderLE.append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) =
    bab.append(array, offset, len)

fun ByteArrayBuilderLE.append(v: Byte) = bab.append(v)
fun ByteArrayBuilderLE.append(vararg v: Byte) = bab.append(*v)
fun ByteArrayBuilderLE.append(vararg v: Int) = bab.append(*v)

inline class ByteArrayBuilderBE(val bab: ByteArrayBuilder)

val ByteArrayBuilderBE.size get() = bab.size
fun ByteArrayBuilderBE.append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) =
    bab.append(array, offset, len)

fun ByteArrayBuilderBE.append(v: Byte) = bab.append(v)
fun ByteArrayBuilderBE.append(vararg v: Byte) = bab.append(*v)
fun ByteArrayBuilderBE.append(vararg v: Int) = bab.append(*v)

private fun ByteArray.u8(o: Int): Int = this[o].toInt() and 0xFF
private fun ByteArray.read16BE(o: Int): Int = (u8(o + 1) shl 0) or (u8(o + 0) shl 8)
private fun ByteArray.read24BE(o: Int): Int = (u8(o + 2) shl 0) or (u8(o + 1) shl 8) or (u8(o + 0) shl 16)
private fun ByteArray.read32BE(o: Int): Int =
    (u8(o + 3) shl 0) or (u8(o + 2) shl 8) or (u8(o + 1) shl 16) or (u8(o + 0) shl 24)

fun ByteArray.readU8(o: Int): Int = u8(o)
fun ByteArray.readU16BE(o: Int): Int = read16BE(o)
fun ByteArray.readU24BE(o: Int): Int = read24BE(o)
fun ByteArray.readS32BE(o: Int): Int = read32BE(o)