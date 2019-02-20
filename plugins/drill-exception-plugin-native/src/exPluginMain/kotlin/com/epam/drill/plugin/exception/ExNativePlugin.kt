package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.NativePluginPart
import com.epam.drillnative.api.SetEventCallbacksP
import com.epam.drillnative.api.disableJvmtiEventException
import com.epam.drillnative.api.enableJvmtiEventException
import com.epam.drillnative.api.jvmtiCallback
import jvmapi.jvmtiEventCallbacks
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf

class ExNativePlugin(override var id: String) : NativePluginPart() {

    override fun unload(id: Long) {
        disableJvmtiEventException()
        println("exception disabled")
    }


    override fun load(id: Long) {
        val message: jvmtiEventCallbacks? = jvmtiCallback()
        message?.Exception = exceptionCallback()
        SetEventCallbacksP(message?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        enableJvmtiEventException()
        println("enabled")
    }

    override fun update(someText: String) {
        println("message from drillNativeCore: $someText")
    }
}