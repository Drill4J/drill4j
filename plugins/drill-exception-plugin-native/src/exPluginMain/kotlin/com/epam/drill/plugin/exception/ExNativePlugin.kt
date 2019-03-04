package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.NativePluginPart
import com.epam.drillnative.api.SetEventCallbacksP
import com.epam.drillnative.api.disableJvmtiEventException
import com.epam.drillnative.api.enableJvmtiEventException
import com.epam.drillnative.api.jvmtiCallback
import jvmapi.jvmtiEventCallbacks
import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ExNativePlugin(override var id: CPointer<ByteVar>) : NativePluginPart<TestExConf>() {
    override fun restring(someText: String): String {
        return String(someText.toCharArray())
    }


    override val confSerializer: KSerializer<TestExConf>
        get() {
            println("runtime calc")
            return TestExConf.serializer()
        }


    override fun unload(id: Long) {
        disableJvmtiEventException()
        println("exception disabled")
    }


    override fun load(id: Long) {
        val message: jvmtiEventCallbacks? = jvmtiCallback()
        message?.Exception = exceptionCallback()
        SetEventCallbacksP(message?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        enableJvmtiEventException()

    }

    override fun updateConfig(someText: TestExConf) {
        println("asdhahdahsd")
        println(String(someText.st.toCharArray()) == "asdasd")
        println(someText)

    }
}

@Serializable
data class TestExConf(val st: String)