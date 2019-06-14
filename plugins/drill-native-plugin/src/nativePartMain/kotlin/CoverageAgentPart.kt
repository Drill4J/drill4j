package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drill.plugin.api.processing.UnloadReason
import kotlinx.serialization.Serializable

@Suppress("unused")
@CName(com.epam.drill.plugin.api.processing.initPlugin)
fun initPlugin(pluginId: String) = CoveragePlugin(pluginId)


@Suppress("unused")
class CoveragePlugin constructor(override var id: String) : NativePart<CoverConfig>() {

    override fun on() {
        println("NATIVE on")
    }

    override fun off() {
        println("NATIVE off")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        println("NATIVE destroyPlugin")
    }


    override fun initPlugin() {
        println("NATIVE initPlugin")
    }

    override var confSerializer: kotlinx.serialization.KSerializer<CoverConfig> = CoverConfig.serializer()
}

@Serializable
data class CoverConfig(val s: String = "")