package com.epam.drill.core.methodbind

import com.epam.drill.core.exec
import com.epam.drill.core.request.parseHttpRequest
import com.epam.drill.core.request.toDrillRequest
import jvmapi.JNIEnvVar
import jvmapi.SetThreadLocalStorage
import jvmapi.jint
import jvmapi.jobject
import kotlinx.cinterop.Arena
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.invoke
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.toLong
import kotlinx.serialization.toUtf8Bytes
import kotlin.math.min

const val SocketDispatcher = "Lsun/nio/ch/SocketDispatcher;"
const val FileDispatcherImpl = "Lsun/nio/ch/FileDispatcherImpl;"

fun read0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): Int {
    initRuntimeIfNeeded()
    val retVal = exec { originalMethod[::read0] }(env, obj, fd, address, len)
    if (retVal > 8) {
        val prefix = address.rawString(min(8, retVal))
        try {
            if (prefix.startsWith("OPTIONS ") ||
                prefix.startsWith("GET ") ||
                prefix.startsWith("HEAD ") ||
                prefix.startsWith("POST ") ||
                prefix.startsWith("PUT ") ||
                prefix.startsWith("PATCH ") ||
                prefix.startsWith("DELETE ") ||
                prefix.startsWith("TRACE ") ||
                prefix.startsWith("CONNECT ")
            ) {
                val request = address.rawString(retVal)
                val parseHttpRequest = parseHttpRequest(request)

                val thread = com.epam.drill.api.currentThread()
                val any = parseHttpRequest.toDrillRequest()
                val data = StableRef.create(any).asCPointer()
                SetThreadLocalStorage(thread, data)
            }

        } catch (ex: Throwable) {
            println(ex.message)
        }
    }
    return retVal
}

fun readv0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): Int =
    read0(env, obj, fd, address, len)


fun write0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): jint {
    initRuntimeIfNeeded()
    val fakeLength: jint
    val fakeBuffer: DirectBufferAddress
    val prefix = address.rawString(min(4, len))
    if (prefix == "HTTP") {
        val spyHeaders = "\ndrill-agent-id: ${exec { agentId }}\ndrill-admin-url: ${exec { adminAddress }}"
        val contentBodyBytes = address.toPointer().toKStringFromUtf8()
        return if (contentBodyBytes.contains("text/html") || contentBodyBytes.contains("application/json")) {
            val replaceFirst = contentBodyBytes.replaceFirst("\n", "$spyHeaders\n")
            val toUtf8Bytes = replaceFirst.toUtf8Bytes()
            val refTo = toUtf8Bytes.refTo(0)
            val scope = Arena()
            fakeBuffer = refTo.getPointer(scope).toLong()
            val additionalSize = spyHeaders.toUtf8Bytes().size
            fakeLength = len + additionalSize
            exec { originalMethod[::write0] }(env, obj, fd, fakeBuffer, fakeLength)
            scope.clear()
            len
        } else {
            exec { originalMethod[::write0] }(env, obj, fd, address, len)
            len
        }
    } else {
        exec { originalMethod[::write0] }(env, obj, fd, address, len)
        return len
    }
}