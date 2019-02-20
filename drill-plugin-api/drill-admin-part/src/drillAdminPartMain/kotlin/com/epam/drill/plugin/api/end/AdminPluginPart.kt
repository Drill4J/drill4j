package com.epam.drill.plugin.api.end

import com.epam.drill.plugin.api.DrillPlugin
import com.epam.drill.plugin.api.message.DrillMessage
import java.util.concurrent.ConcurrentHashMap

abstract class AdminPluginPart(val sender: WsService, override val id: String) : DrillPlugin() {
    abstract suspend fun processData(dm: DrillMessage): Any

}
