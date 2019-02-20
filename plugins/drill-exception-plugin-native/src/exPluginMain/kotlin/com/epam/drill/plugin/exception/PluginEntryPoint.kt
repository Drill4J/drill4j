package com.epam.drill.plugin.exception

import com.epam.drillnative.api.addPluginToRegistry


@Suppress("FunctionName", "UNUSED_PARAMETER", "unused")
@CName("JNI_OnLoad")
fun pluginSetup(vm: Long, reservedPtr: Long): Int {
//    val findClass = FindClass("java/lang/Object")

    //fixme fix this hardcode...!!!!
    val cls = ExNativePlugin("except-ions")
    cls.load(1)
    addPluginToRegistry(cls)

    return 65542
}