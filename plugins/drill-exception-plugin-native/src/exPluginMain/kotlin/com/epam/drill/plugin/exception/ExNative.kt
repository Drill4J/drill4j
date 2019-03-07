package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drill.plugin.api.processing.Reason
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
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

class ExNative(override var id: CPointer<ByteVar>) : NativePart<ExceptionConfig>() {
    override val confSerializer: KSerializer<ExceptionConfig> = ExceptionConfig.serializer()


    override fun initPlugin() {
        val jvmtiCallback = jvmtiCallback()
        jvmtiCallback?.Exception = exceptionCallback()
        SetEventCallbacksP(jvmtiCallback?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    }

    override fun destroyPlugin(reason: Reason) {
        val jvmtiCallback = jvmtiCallback()
        jvmtiCallback?.Exception = null
        SetEventCallbacksP(jvmtiCallback?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    }


    override fun off() {
        disableJvmtiEventException()
    }

    override fun on() {
        enableJvmtiEventException()
    }


}


@Serializable
data class ExceptionConfig(@Optional val id: String = "", val blackList: Set<String>, @Optional val enabled: Boolean = false)