package com.epam.drill.plugins.exceptions

import com.epam.drill.plugin.api.processing.AgentPluginPart


@Suppress("unused")
/**
 * @author Igor Kuzminykh on 8/8/17.
 */
class LogExceptions(override val id: String) : AgentPluginPart() {
    override fun load() {
        println("Plugin $id loaded")
    }

    override fun init(nativePluginPartPath: String) {
        println("try to load native $nativePluginPartPath")
        super.init(nativePluginPartPath)
    }

    override fun unload() {
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}