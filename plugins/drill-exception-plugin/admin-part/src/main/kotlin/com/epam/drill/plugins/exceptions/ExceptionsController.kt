package com.epam.drill.plugins.exceptions


import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlin.reflect.KClass

@Suppress("unused")

class ExceptionsController(private val ws: WsService) : AdminPluginPart(ws) {
    override val configClass: KClass<out PluginBean>
        get() = PluginBean::class


    override suspend fun processData(dm: DrillMessage): Any {
        val sessionId = dm.sessionId
        ws.convertAndSend(pluginInfo().id + (sessionId ?: ""), dm.content!!)
        return ""
    }


    override fun callBack() {
        println("call")
    }
}
