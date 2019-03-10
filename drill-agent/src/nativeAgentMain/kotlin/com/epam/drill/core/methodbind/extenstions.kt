package com.epam.drill.core.methodbind

import com.soywiz.kmem.buildByteArray
import jvmapi.jlong
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.toCPointer


typealias DirectBufferAddress = jlong

fun DirectBufferAddress.toPointer() = this.toCPointer<ByteVar>()!!
fun DirectBufferAddress.readBytes(end: Int): ByteArray {
    val bufPointer = this.toPointer()
    return buildByteArray {
        for (i in 0 until end) {
            append(bufPointer[i])
        }
    }
}
fun DirectBufferAddress.rawString(end: Int): String {
    return this.readBytes(end).stringFromUtf8()
}