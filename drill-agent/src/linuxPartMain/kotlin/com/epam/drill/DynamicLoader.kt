package com.epam.drill

import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drill.plugin.api.processing.initPlugin
import kotlinx.cinterop.*
import platform.posix.RTLD_LAZY
import platform.posix.dlopen
import platform.posix.dlsym


fun loadNativePlugin(pluginId: String, path: String) = memScoped {
    var pluginInstance: NativePart<*>? = null
    val handle = dlopen(path, RTLD_LAZY)
    if (handle != null) {
        val initPlugin = dlsym(handle, initPlugin)
        pluginInstance = initPlugin?.reinterpret<CFunction<(CPointer<ByteVar>) -> COpaquePointer>>()
            ?.invoke(pluginId.cstr.getPointer(this))?.asStableRef<NativePart<*>>()?.get()
    }
    pluginInstance
}