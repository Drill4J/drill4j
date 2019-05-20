package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drill.plugin.api.processing.UnloadReason
import com.epam.drillnative.api.SetEventCallbacksP
import com.epam.drillnative.api.disableJvmtiEventException
import com.epam.drillnative.api.enableJvmtiEventException
import com.epam.drillnative.api.jvmtiCallback
import jvmapi.jvmtiEventCallbacks
import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

class ExNative(override var id: CPointer<ByteVar>) : NativePart<ExceptionConfig>() {
    override val confSerializer: KSerializer<ExceptionConfig> = ExceptionConfig.serializer()


    override fun initPlugin() {
        val jvmtiCallback = jvmtiCallback()
//        jvmtiCallback?.Exception = exceptionCallback()
        jvmtiCallback?.Exception = staticCFunction(::exceptionCallback)
        SetEventCallbacksP(jvmtiCallback?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        val jvmtiCallback = jvmtiCallback()
        jvmtiCallback?.Exception = null
        SetEventCallbacksP(jvmtiCallback?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    }


    override fun off() {
        disableJvmtiEventException()
    }

    override fun on() {
        enableJvmtiEventException()
        println("Native is enavble!")
    }


}


@Serializable
data class ExceptionConfig(@Optional val id: String = "", val blackList: Set<String>, @Optional val enabled: Boolean = false)