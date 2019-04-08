package com.epam.drill.plugin.api.processing

import kotlinx.serialization.json.JSON

abstract class InstrumentedPlugin<T, A> : AgentPart<T>() {

    override fun initPlugin() {
        println("empty initPlugin")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        println("empty destroyPlugin")
    }


    override fun on() {
        println("empty On")
    }

    override fun off() {
        println("empty off")
    }

    abstract fun doAction(action: A)

    fun doRawAction(action: String) {
        doAction(JSON.parse(actionSerializer, action))
    }

    abstract var actionSerializer: kotlinx.serialization.KSerializer<A>

    abstract fun instrument(className: String, initialBytes: ByteArray): ByteArray

}