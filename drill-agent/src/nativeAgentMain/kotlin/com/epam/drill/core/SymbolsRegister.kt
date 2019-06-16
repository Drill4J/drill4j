@file:Suppress("unused")

package com.epam.drill.core

import com.epam.drill.api.drillRequest
import com.epam.drill.api.sendToSocket
import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.jvmapi.toKString
import jvmapi.FindClass
import jvmapi.GetLoadedClasses
import jvmapi.GetObjectArrayElement
import jvmapi.JNIEnv
import jvmapi.NewObjectArray
import jvmapi.NewStringUTF
import jvmapi.SetObjectArrayElement
import jvmapi.jclassVar
import jvmapi.jint
import jvmapi.jintVar
import jvmapi.jobject
import jvmapi.jobjectArray
import jvmapi.jstring
import jvmapi.jvmtiEnvVar
import jvmapi.jvmtiError
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return com.epam.drill.jvmapi.currentEnvs()
}

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? {
    return com.epam.drill.jvmapi.jvmtii()
}

@ExperimentalUnsignedTypes
@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String): jvmtiError {
    return com.epam.drill.jvmapi.checkEx(errCode, funName)
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_processing_Sender_sendMessage")
fun sendFromJava(env: JNIEnv, thiz: jobject, pluginId: jstring, message: jstring) {
    sendToSocket(pluginId.toKString()!!, message.toKString()!!)
}


@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_session_DrillRequest_currentSession")
fun currentsession4java(env: JNIEnv, thiz: jobject): jobject? {
    return NewStringUTF(drillRequest()?.drillSessionId)

}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_session_DrillRequest_get")
fun getHeader4java(env: JNIEnv, thiz: jobject, key: jstring): jobject? {
    return NewStringUTF(drillRequest()?.get(key.toKString()))
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_session_DrillRequest_RetransformClasses")
fun RetransformClasses(env: JNIEnv, thiz: jobject, count: jint, classes: jobjectArray) = memScoped {
    val allocArray = allocArray<jclassVar>(count) { index ->
        value = GetObjectArrayElement(classes, index)
    }
    jvmapi.RetransformClasses(count, allocArray)
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_session_DrillRequest_GetAllLoadedClasses")
fun GetAllLoadedClasses(env: JNIEnv, thiz: jobject) = memScoped {
    val cout = alloc<jintVar>()
    val classes = alloc<CPointerVar<jclassVar>>()
    GetLoadedClasses(cout.ptr, classes.ptr)
    println()
    val reinterpret = classes.value!!
    val len = cout.value
    val newByteArray = NewObjectArray(len, FindClass("java/lang/Class"), null)

    for (i in 0 until cout.value) {
        val cPointer = reinterpret[i]
        SetObjectArrayElement(newByteArray, i, cPointer)
    }
    newByteArray
}
