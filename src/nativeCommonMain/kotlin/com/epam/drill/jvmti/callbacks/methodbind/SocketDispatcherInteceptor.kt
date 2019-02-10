package com.epam.drill.jvmti.callbacks.methodbind

import com.epam.drill.jvmti.request.DrillRequest
import com.epam.drill.jvmti.request.RetrieveDrillSessionFromRequest
import com.epam.drill.jvmti.thread.currentThread
import com.soywiz.kmem.buildByteArray
import jvmapi.*
import kotlinx.cinterop.*

@Suppress("unused")
@CName("SCread0")
fun read0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): jint {
    initRuntimeIfNeeded()
    val retVal = super_read0(env, obj, fd, address, len)
    if (retVal > 0)
        try {
            val request = address.readBytes(retVal).stringFromUtf8()
            if (request.contains("Cookie") || request.contains("cookie")) {
                val currentThread = currentThread()
                val drillRequestPointer = StableRef.create(DrillRequest(RetrieveDrillSessionFromRequest(request))).asCPointer()
                SetThreadLocalStorage(currentThread, drillRequestPointer)
            }

        } catch (ex: Throwable) {
            println(ex.message)
        }
    return retVal
}

@Suppress("FunctionName")
private fun super_read0(
    env: CPointer<JNIEnvVar>,
    obj: jobject,
    fd: jobject,
    address: jlong,
    len: jint
) = SCReadMethodAddress!!(env, obj, fd, address, len)

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