@file:Suppress("unused")

package com.epam.drill.core

import com.epam.drill.api.drillRequest
import com.epam.drill.api.sendToSocket
import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.jvmapi.toKString
import jvmapi.*
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr

@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return com.epam.drill.jvmapi.currentEnvs()
}

@ExperimentalUnsignedTypes
@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String): jvmtiError {
    return com.epam.drill.jvmapi.checkEx(errCode, funName)
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_processing_Sender_sendMessage")
fun sendFromJava(env: JNIEnv, thiz: jobject, pluginId: jstring, message: jstring) {
    sendToSocket(pluginId.toKString()!!.cstr.getPointer(Arena()), message.toKString()!!.cstr.getPointer(Arena()))
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