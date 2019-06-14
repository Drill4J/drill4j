package com.epam.drill

import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set


fun loadNativePlugin(pluginId: String, path: String) = memScoped {
    var pluginInstance: NativePart<*>? = null
    void* handle = dlopen(path, RTLD_LAZY);
    if (handle == NULL) {
        fprintf(stderr, "Could not open plugin: %s\n", dlerror());
        return 1;
    }
    plugin_func* f = dlsym(handle, "plugin_func");
    //TODO do it fo linux
    pluginInstance
}

private fun String.toLPCWSTR(ms: MemScope): CArrayPointer<UShortVar> {
    val length = this.length
    val allocArray = ms.allocArray<UShortVar>(length.toLong())
    for (i in 0 until length) {
        allocArray[i] = this[i].toShort().toUShort()
    }
    return allocArray
}