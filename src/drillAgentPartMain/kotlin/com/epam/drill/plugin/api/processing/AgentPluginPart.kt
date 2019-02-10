package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin

abstract class AgentPluginPart : DrillPlugin() {

    override fun callBack() {
//    empty
    }

    fun sendData(message: String?) {
        Sender.send(pluginInfo().id, message)
    }

    fun tryUnload() {
        unload()
        //fixme log
//        logDebug("plugin ${pluginInfo().id} was unloaded")
    }

    abstract fun unload()


}
