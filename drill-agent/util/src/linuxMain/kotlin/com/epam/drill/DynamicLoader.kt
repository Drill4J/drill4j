package com.epam.drill

import com.epam.drill.plugin.api.processing.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import platform.posix.*

fun loadNativePlugin(
    pluginId: String,
    path: String,
    sender: CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>
) = memScoped {
    var pluginInstance: NativePart<*>? = null
    val handle = dlopen(path, RTLD_LAZY)
    if (handle != null) {
        val initPlugin = dlsym(handle, initPlugin)
        val callbacks: jvmtiEventCallbacks? = gjavaVMGlob?.pointed?.callbackss
        val reinterpret =
            initPlugin?.reinterpret<CFunction<(CPointer<ByteVar>, CPointer<com.epam.drill.jvmapi.gen.jvmtiEnvVar>?, CPointer<JavaVMVar>?, CPointer<jvmtiEventCallbacks>?, CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>) -> COpaquePointer>>()
        val id = pluginId.cstr.getPointer(this)
        val jvmti = gdata?.pointed?.jvmti
        val jvm = gjavaVMGlob?.pointed?.jvm
        val clb = callbacks?.ptr
        pluginInstance =
            reinterpret?.invoke(
                id,
                jvmti,
                jvm,
                clb,
                sender
            )?.asStableRef<NativePart<*>>()?.get()
    }
    pluginInstance
}