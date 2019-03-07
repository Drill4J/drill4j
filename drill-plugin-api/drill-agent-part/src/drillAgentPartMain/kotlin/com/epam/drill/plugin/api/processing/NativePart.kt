package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.json.JSON

actual abstract class NativePart<T> {

    actual abstract val confSerializer: KSerializer<T>
    actual fun updateRawConfig(someText: String) {
    }

}

abstract class DummyAgentPart(override val id: String) : AgentPart<Any>() {
    override fun initPlugin() {
        println("[JAVA SIDE] Plugin $id loaded")
    }

    override fun destroyPlugin(reason: Reason) {
        println("[JAVA SIDE] Plugin '$id' unloaded")
    }

    override fun on() {
        println("[JAVA SIDE] Plugin $id enabled")
    }


    override fun off() {
        println("[JAVA SIDE] Plugin $id disabled")
    }

    override fun updateRawConfig(configs: String) {
        println("update stub")
        //empty
    }

    override var confSerializer: KSerializer<Any>
        get() = TODO("stub")
        set(value) {}
}

actual abstract class AgentPart<T> : DrillPlugin(), Switchable, Lifecycle {
    var isStub = false
    actual var enabled: Boolean = false
    var config: T? = null


    actual open fun init(nativePluginPartPath: String) {
        loadNativePart(nativePluginPartPath)
    }


    actual fun load() {
        initPlugin()
        on()
    }

    actual fun unload(reason: Reason) {
        off()
        destroyPlugin(reason)
    }

    actual abstract override fun on()
    actual abstract override fun off()


    external fun loadNative(ss: Long)
    actual var np: NativePart<T>? = null


    open fun updateRawConfig(configs: String) {
        config = JSON().parse(confSerializer, configs)
    }


    actual abstract var confSerializer: kotlinx.serialization.KSerializer<T>
    actual abstract override fun initPlugin()

    actual abstract override fun destroyPlugin(reason: Reason)
}