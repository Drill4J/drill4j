package com.epam.drill.plugin.exception

import com.epam.drillnative.api.addPluginToRegistry
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr


@Suppress("FunctionName", "UNUSED_PARAMETER", "unused")
@CName("JNI_OnLoad")
fun pluginSetup(vm: Long, reservedPtr: Long): Int {

    //fixme fix this hardcode...!!!!
    val cls = ExNative("except-ions".cstr.getPointer(Arena()))
    cls.load()
    addPluginToRegistry(cls)

    return 65542
}