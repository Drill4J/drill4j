package com.epam.drill.jvmti.callbacks.vminit

import com.epam.kjni.core.GlobState
import jvmapi.JNIEnvVar
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped


@ExperimentalUnsignedTypes
@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun initVmEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, mainThread: jthread?) {
//    RunAgentThread(java.lang.Thread().javaObject, drillMainThreadCallback(), null, JVMTI_THREAD_MAX_PRIORITY.toInt())

    memScoped {
        val pluginLoaderClass =
            GlobState.jni.FindClass?.invoke(GlobState.env, "com/epam/drill/PluginLoader".cstr.getPointer(this))
        val instanceField = GlobState.jni.GetStaticFieldID?.invoke(
            GlobState.env,
            pluginLoaderClass,
            "INSTANCE".cstr.getPointer(this),
            "Lcom/epam/drill/PluginLoader;".cstr.getPointer(this)
        )
        val pluginLoader = GlobState.jni.GetStaticObjectField?.invoke(GlobState.env, pluginLoaderClass, instanceField)
        val registerMethodId = GlobState.jni.GetMethodID?.invoke(
            GlobState.env,
            pluginLoaderClass,
            "register".cstr.getPointer(this),
            "()V".cstr.getPointer(this)
        )
        GlobState.jni.CallObjectMethodV?.invoke(GlobState.env, pluginLoader, registerMethodId, null)
    }

}
