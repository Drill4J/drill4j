//package com.epam.drill.core.callbacks.methodbind
//
//import com.epam.drill.api.currentThread
//import com.epam.drill.api.drillRequest
//import com.epam.drill.core.request.RetrieveDrillSessionFromRequest
//import com.soywiz.kmem.buildByteArray
//import jvmapi.*
//import kotlinx.cinterop.*
//
//@Suppress("unused")
//@CName("SCread0")
//fun read0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): jint {
//    initRuntimeIfNeeded()
//    val retVal = super_read0(env, obj, fd, address, len)
//    if (retVal > 0)
//        try {
//            val request = address.readBytes(retVal).stringFromUtf8()
//
//            //fixme!!!!! HTTP!
//            if (request.contains("Cookie") || request.contains("cookie")) {
//                val currentThread = currentThread()
//                val drillRequestPointer =
//                    StableRef.create(RetrieveDrillSessionFromRequest(request)).asCPointer()
//                SetThreadLocalStorage(currentThread, drillRequestPointer)
//            }
//
//        } catch (ex: Throwable) {
//            println(ex.message)
//        }
//    return retVal
//}
//
//
//@Suppress("unused")
//@CName("SCwritev0")
//fun writev0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): jint {
//    initRuntimeIfNeeded()
//    println("writev!!")
//    val convertAddressToBuffers = convertAddressToBuffers(address)
//
//    var next_offset = 0
//    var next_index = 0
//    var rem = ((128 * 1024) - 1)
//    println("____________________")
//    while (next_index < len && rem > 0) {
//        var iov_len = convertAddressToBuffers!![next_index].iov_len - next_offset;
//        var ptr = convertAddressToBuffers[next_index].iov_base.toCPointer<ByteVar>();
//        ptr += next_offset;
//
//        if (iov_len > rem) {
//            iov_len = rem;
//            next_offset += rem;
//        } else {
//            next_index++
//            next_offset = 0
//        }
//        val w = ptr
//        println(w?.toKString()?.contains("HTTP/1.1"))
//        println(next_index)
//        val lenw = iov_len;
//        println(lenw)
//        rem -= iov_len;
//    }
//    val retVal = super_writev0(env, obj, fd, address, len)
//    return retVal
//}
//
//@Suppress("unused")
//@CName("SCwrite0")
//fun write0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): jint {
//    initRuntimeIfNeeded()
//    var fakeLength = len
//    var fakeBuffer = address
//    var contentBodyBytes = address.readBytes(len).stringFromUtf8()
//    if (contentBodyBytes.contains("HTTP/1.1")) {
//        val drillRequest = drillRequest()
//
//
//        val map = contentBodyBytes.lines().map {
//            if (it.startsWith("HTTP/1.1 200")) {
//                it + "\n" + "DrillServerAddress: ${drillRequest?.host + drillRequest?.drillSessionId}"
//            } else {
//                it
//            }
//        }
//
//        val toUtf8 = map.joinToString(separator = "\n").toUtf8()
//        try {
//            val alloc = nativeHeap.allocArray<ByteVar>(toUtf8.size) { index -> value = toUtf8[index] }
//            fakeBuffer = alloc.toLong()
//            fakeLength = toUtf8.size
//        } catch (ex: Throwable) {
//            ex.printStackTrace()
//        }
//    }
//
//    val retVal = super_write0(env, obj, fd, fakeBuffer, fakeLength)
//    return len
//}
//
//@Suppress("FunctionName")
//private fun super_read0(
//    env: CPointer<JNIEnvVar>,
//    obj: jobject,
//    fd: jobject,
//    address: jlong,
//    len: jint
//) = SCReadMethodAddress!!(env, obj, fd, address, len)
//
//@Suppress("FunctionName")
//private fun super_write0(
//    env: CPointer<JNIEnvVar>,
//    obj: jobject,
//    fd: jobject,
//    address: jlong,
//    len: jint
//) = SCWriteMethodAddress!!(env, obj, fd, address, len)
//
//@Suppress("FunctionName")
//private fun super_writev0(
//    env: CPointer<JNIEnvVar>,
//    obj: jobject,
//    fd: jobject,
//    address: jlong,
//    len: jint
//) = SCWritevMethodAddress!!(env, obj, fd, address, len)
//
//typealias DirectBufferAddress = jlong
//
//fun DirectBufferAddress.toPointer() = this.toCPointer<ByteVar>()!!
//fun DirectBufferAddress.readBytes(end: Int): ByteArray {
//    val bufPointer = this.toPointer()
//    return buildByteArray {
//        for (i in 0 until end) {
//            append(bufPointer[i])
//        }
//    }
//}