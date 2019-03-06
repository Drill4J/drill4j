package com.epam.drill.plugins.exceptions

import com.epam.drill.plugin.api.processing.AgentPluginPart


@Suppress("unused")
/**
 * @author Igor Kuzminykh on 8/8/17.
 */
class LogExceptions(override val id: String) : AgentPluginPart<Any>() {
    override fun updateConfig(config: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var confSerializer: kotlinx.serialization.KSerializer<Any>? = null

    override fun on() {
        println("Plugin $id loaded")
    }

    override fun init(nativePluginPartPath: String) {
        println("try to load native $nativePluginPartPath")
        super.init(nativePluginPartPath)
    }

    override fun off() {
        println("JAVA SIDE: Plugin '$id' unloaded")
    }


}