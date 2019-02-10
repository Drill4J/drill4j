@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.jvmti

import com.epam.drill.jvmti.util.disableJvmtiEventException
import com.epam.drill.jvmti.util.enableJvmtiEventException
import jvmapi.JNIEnv
import jvmapi.jobject

object NativeApi

@CName("Java_com_epam_drill_plugin_api_processing_natives_NativeApi_enableJvmtiEventException")
fun enableJvmtiEventException(env: JNIEnv, thisObject: jobject) {
    enableJvmtiEventException()
}

@CName("Java_com_epam_drill_plugin_api_processing_natives_NativeApi_disableJvmtiEventException")
fun disableJvmtiEventException(env: JNIEnv, thisObject: jobject) {
    disableJvmtiEventException()
}

