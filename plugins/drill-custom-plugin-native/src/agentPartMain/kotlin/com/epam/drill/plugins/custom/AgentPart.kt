package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.AgentPluginPart


@Suppress("unused")
/**
 * @author Denis Domashenko on 2/22/19.
 */
class AgentPart(override val id: String) : AgentPluginPart() {
    override fun load() {
        println("Plugin $id loaded")
    }


    override fun unload() {
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}