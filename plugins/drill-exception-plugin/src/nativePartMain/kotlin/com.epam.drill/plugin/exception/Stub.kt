@file:Suppress("unused")

package com.epam.drill.plugin.exception

import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.jvmapi.ex
import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drill.plugin.api.processing.NativePluginApi
import com.epam.drill.plugin.api.processing.api
import com.epam.drill.plugin.api.processing.natContex
import com.epam.drill.plugin.api.processing.plugin
import com.epam.drill.plugin.api.processing.pluginApi
import jvmapi.JNIEnv
import jvmapi.JNIEnvVar
import jvmapi.JNI_VERSION_1_6
import jvmapi.JavaVMVar
import jvmapi.jvmtiEnvVar
import jvmapi.jvmtiError
import jvmapi.jvmtiEventCallbacks
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.native.concurrent.TransferMode


/**
 * These stuff should be generated for native plugins via compiler plugin API.
 * Will be implemented during design of drill hub*/

@CName(com.epam.drill.plugin.api.processing.initPlugin)
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