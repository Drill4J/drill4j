package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.*
import kotlinx.serialization.*

abstract class PluginRepresenter : AgentPart<Any, Any>() {
    override val serDe: SerDe<Any>
        get() = TODO()
    @Suppress("UNUSED_PARAMETER")
    override val confSerializer: KSerializer<Any>
        get() = TODO()


    override fun initPlugin() = TODO()

    override fun destroyPlugin(unloadReason: UnloadReason) = TODO()

    override suspend fun doAction(action: Any) = TODO()
}