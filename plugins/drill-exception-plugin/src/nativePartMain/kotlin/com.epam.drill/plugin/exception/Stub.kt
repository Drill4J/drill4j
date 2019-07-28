@file:Suppress("unused")

package com.epam.drill.plugin.exception

import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*
import kotlinx.serialization.json.*
import kotlin.native.concurrent.*


/**
 * These stuff should be generated for native plugins via compiler plugin API.
 * Will be implemented during design of drill hub*/

@CName(initPlugin)
fun initPlugin(
    pluginId: String,
    jvmti: CPointer<jvmtiEnvVar>?,
    jvm: CPointer<JavaVMVar>?,
    clb: CPointer<jvmtiEventCallbacks>?,
    sender: CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>
): NativePart<ExceptionConfig> {
    val exceptionNativePlugin = ExceptionNativePlugin(pluginId)
    natContex.execute(
        TransferMode.UNSAFE,
        { exceptionNativePlugin to NativePluginApi(pluginId, jvmti, jvm, clb, sender) }) {
        plugin = it.first
        api = it.second
    }.result
    return exceptionNativePlugin
}


@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer? {
    return memScoped {
        val vms = pluginApi { jvm }!!
        val vmFns = vms.pointed.value!!.pointed
        val jvmtiEnvPtr = alloc<CPointerVar<JNIEnvVar>>()
        vmFns.AttachCurrentThread!!(vms, jvmtiEnvPtr.ptr.reinterpret(), null)
        val value: CPointer<CPointerVarOf<JNIEnv>>? = jvmtiEnvPtr.value
        ex = value
        JNI_VERSION_1_6
        value!!
    }
}

@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String): jvmtiError {
    return com.epam.drill.jvmapi.checkEx(errCode, funName)
}

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? {
    return pluginApi { jvmti }
}


@SharedImmutable
val js = Json(JsonConfiguration.Stable)