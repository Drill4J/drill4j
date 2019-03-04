@file:Suppress("unused")

package com.epam.drill.core

import com.epam.drill.api.sendToSocket
import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.toKString
import jvmapi.JNIEnv
import jvmapi.jobject
import jvmapi.jstring
import jvmapi.jvmtiError
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

@CName("Java_com_epam_drill_plugin_api_processing_Sender_sendMessage")
fun sendFromJava(env: JNIEnv, thiz: jobject, pluginId: jstring, message: jstring) {
    sendToSocket(pluginId.toKString()!!.cstr.getPointer(Arena()), message.toKString()!!.cstr.getPointer(Arena()))
}