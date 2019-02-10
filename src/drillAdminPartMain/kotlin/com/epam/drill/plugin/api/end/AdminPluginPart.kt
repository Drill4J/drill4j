package com.epam.drill.plugin.api.end

import com.epam.drill.plugin.api.DrillPlugin
import com.epam.drill.plugin.api.message.DrillMessage
import java.util.concurrent.ConcurrentHashMap

abstract class AdminPluginPart(val sender: WsService) : DrillPlugin() {
    abstract suspend fun processData(dm: DrillMessage): Any

    fun aa(){

    }
}
