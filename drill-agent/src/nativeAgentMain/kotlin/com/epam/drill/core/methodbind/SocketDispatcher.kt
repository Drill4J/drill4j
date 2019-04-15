package com.epam.drill.core.methodbind

import com.epam.drill.api.drillCRequest
import com.epam.drill.core.exec
import com.epam.drill.core.request.parseHttpRequest
import com.epam.drill.core.request.toDrillRequest
import jvmapi.JNIEnvVar
import jvmapi.SetThreadLocalStorage
import jvmapi.jint
import jvmapi.jobject
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.invoke
import kotlin.math.min

const val SocketDispatcher = "Lsun/nio/ch/SocketDispatcher;"
const val FileDispatcherImpl = "Lsun/nio/ch/FileDispatcherImpl;"

fun read0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): Int {
    initRuntimeIfNeeded()
    val retVal = exec { originalMethod[::read0](env, obj, fd, address, len) }
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
                drillCRequest()?.dispose()
                val request = address.rawString(retVal)
                val parseHttpRequest = parseHttpRequest(request)

                SetThreadLocalStorage(
                    com.epam.drill.api.currentThread(),
                    StableRef.create(parseHttpRequest.toDrillRequest()).asCPointer()
                )
            }

        } catch (ex: Throwable) {
            println(ex.message)
        }
    }
    return retVal
}

fun readv0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): Int =
    read0(env, obj, fd, address, len)
