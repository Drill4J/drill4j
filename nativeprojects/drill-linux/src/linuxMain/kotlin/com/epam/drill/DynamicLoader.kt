package com.epam.drill

import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drill.plugin.api.processing.initPlugin
import com.epam.drill.jvmapi.gen.JavaVMVar
import com.epam.drill.jvmapi.gen.gdata
import com.epam.drill.jvmapi.gen.gjavaVMGlob
import com.epam.drill.jvmapi.gen.jvmtiEventCallbacks
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import platform.posix.RTLD_LAZY
import platform.posix.dlopen
import platform.posix.dlsym


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