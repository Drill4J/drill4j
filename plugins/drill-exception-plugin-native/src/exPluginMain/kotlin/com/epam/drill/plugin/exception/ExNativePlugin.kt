package com.epam.drill.plugin.exception

import com.epam.drill.exception.ExceptionConfig
import com.epam.drill.plugin.api.processing.NativePluginPart
import com.epam.drillnative.api.SetEventCallbacksP
import com.epam.drillnative.api.disableJvmtiEventException
import com.epam.drillnative.api.enableJvmtiEventException
import com.epam.drillnative.api.jvmtiCallback
import jvmapi.jvmtiEventCallbacks
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.serialization.KSerializer

class ExNativePlugin(override var id: CPointer<ByteVar>) : NativePluginPart<ExceptionConfig>() {

    override val confSerializer: KSerializer<ExceptionConfig> = ExceptionConfig.serializer()

    override fun unload(id: Long) {
        disableJvmtiEventException()
        println("exception disabled")
        println(config?.blackList)
    }

    override fun load(id: Long) {
        val jvmtiCallback = jvmtiCallback()
        jvmtiCallback?.Exception = exceptionCallback()
        SetEventCallbacksP(jvmtiCallback?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        enableJvmtiEventException()
    }

}