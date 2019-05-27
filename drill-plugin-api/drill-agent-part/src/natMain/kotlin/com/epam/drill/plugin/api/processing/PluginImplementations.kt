package com.epam.drill.plugin.api.processing

import kotlinx.serialization.KSerializer

abstract class PluginRepresenter : AgentPart<Any,Any>() {
    @Suppress("UNUSED_PARAMETER")
    override var confSerializer: KSerializer<Any>
        get() = throw NotImplementedError()
        set(value) {}

    override fun initPlugin() {
        throw NotImplementedError()
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        throw NotImplementedError()
    }

}