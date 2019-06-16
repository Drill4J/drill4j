package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.plugin
import com.epam.drill.plugin.api.processing.pluginApi
import jvmapi.JNIEnvVar
import jvmapi.jlocation
import jvmapi.jmethodID
import jvmapi.jobject
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.CPointer


@Suppress("UNUSED_PARAMETER")
fun exceptionCallback(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    thread: jthread?,
    method: jmethodID?,
    location: jlocation,
    exception: jobject?,
    catchMethod: jmethodID?,
    catchLocation: jlocation
) {
    initRuntimeIfNeeded()
    method ?: return
    thread ?: return
    exception ?: return
    (pluginApi { plugin } as? ExceptionNativePlugin)?.exception(thread, method, exception)
}

