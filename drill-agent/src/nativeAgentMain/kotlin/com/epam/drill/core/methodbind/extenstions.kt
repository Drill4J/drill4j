package com.epam.drill.core.methodbind

import jvmapi.*
import kotlinx.cinterop.*


typealias DirectBufferAddress = jlong

fun DirectBufferAddress.toPointer() = this.toCPointer<ByteVar>()!!
fun DirectBufferAddress.readBytes(end: Int) = this.toPointer().readBytes(end)


fun DirectBufferAddress.rawString(end: Int): String {
    return this.readBytes(end).stringFromUtf8()
}