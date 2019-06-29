package com.epam.drill.core.methodbind

import jvmapi.jlong
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCPointer


typealias DirectBufferAddress = jlong

fun DirectBufferAddress.toPointer() = this.toCPointer<ByteVar>()!!
fun DirectBufferAddress.readBytes(end: Int) = this.toPointer().readBytes(end)


fun DirectBufferAddress.rawString(end: Int): String {
    return this.readBytes(end).stringFromUtf8()
}