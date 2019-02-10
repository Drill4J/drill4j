package com.epam.drill.storage

import com.epam.drill.plugin.api.DrillPlugin
import java.util.*

object PluginsStorage {

    val pluginsMapping = HashMap<String, DrillPlugin>()

    fun addPlugin(pb: DrillPlugin) {
        this.pluginsMapping[pb.pluginInfo().id] = pb
    }


}
