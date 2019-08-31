package com.epam.drill


class MemBuffer(val data: ByteArray)

inline val MemBuffer.size: Int get() = data.size

typealias DataBuffer = MemBuffer

fun DataBuffer.getByte(index: Int): Byte = data.get(index)
fun DataBuffer.getShort(index: Int): Short = data.getShortAt(index)
fun DataBuffer.getInt(index: Int): Int = data.getIntAt(index)
fun DataBuffer.getFloat(index: Int): Float = data.getFloatAt(index)
fun DataBuffer.getDouble(index: Int): Double = data.getDoubleAt(index)
fun DataBuffer.setByte(index: Int, value: Byte): Unit = data.set(index, value)
fun DataBuffer.setShort(index: Int, value: Short): Unit = data.setShortAt(index, value)
fun DataBuffer.setInt(index: Int, value: Int): Unit = data.setIntAt(index, value)
fun DataBuffer.setFloat(index: Int, value: Float): Unit = data.setFloatAt(index, value)
fun DataBuffer.setDouble(index: Int, value: Double): Unit = data.setDoubleAt(index, value)

class Int8Buffer(val mbuffer: MemBuffer, val byteOffset: Int, val size: Int) {
    companion object {
        const val SIZE = 1
    }


    fun getByteIndex(index: Int) = byteOffset + index * SIZE
}

operator fun Int8Buffer.get(index: Int): Byte = mbuffer.getByte(getByteIndex(index))
operator fun Int8Buffer.set(index: Int, value: Byte): Unit = mbuffer.setByte(getByteIndex(index), value)

class Int16Buffer(val mbuffer: MemBuffer, val byteOffset: Int, val size: Int) {
    companion object {
        const val SIZE = 2
    }

    fun getByteIndex(index: Int) = byteOffset + index * SIZE
}

operator fun Int16Buffer.get(index: Int): Short = mbuffer.getShort(getByteIndex(index))
operator fun Int16Buffer.set(index: Int, value: Short): Unit = mbuffer.setShort(getByteIndex(index), value)

class Int32Buffer(val mbuffer: MemBuffer, val byteOffset: Int, val size: Int) {
    companion object {
        const val SIZE = 4
    }

    fun getByteIndex(index: Int) = byteOffset + index * SIZE
}

operator fun Int32Buffer.get(index: Int): Int = mbuffer.getInt(getByteIndex(index))
operator fun Int32Buffer.set(index: Int, value: Int): Unit = mbuffer.setInt(getByteIndex(index), value)

class Float32Buffer(val mbuffer: MemBuffer, val byteOffset: Int, val size: Int) {
    companion object {
        const val SIZE = 4
    }

    fun getByteIndex(index: Int) = byteOffset + index * SIZE
}

operator fun Float32Buffer.get(index: Int): Float = mbuffer.getFloat(getByteIndex(index))
operator fun Float32Buffer.set(index: Int, value: Float): Unit = mbuffer.setFloat(getByteIndex(index), value)

class Float64Buffer(val mbuffer: MemBuffer, val byteOffset: Int, val size: Int) {
    companion object {
        const val SIZE = 8
    }

    fun getByteIndex(index: Int) = byteOffset + index * SIZE
}

operator fun Float64Buffer.get(index: Int): Double = mbuffer.getDouble(getByteIndex(index))
operator fun Float64Buffer.set(index: Int, value: Double): Unit = mbuffer.setDouble(getByteIndex(index), value)
