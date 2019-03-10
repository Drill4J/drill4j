package com.epam.drill.plugin.api.processing

import kotlinx.serialization.KSerializer


abstract class DummyAgentPart(override val id: String) : AgentPart<Any>() {

    override fun initPlugin() {
        println("[JAVA SIDE] Plugin $id loaded")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
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

    @Suppress("UNUSED_PARAMETER")
    override var confSerializer: KSerializer<Any>
        get() = TODO("stub")
        set(value) {}
}