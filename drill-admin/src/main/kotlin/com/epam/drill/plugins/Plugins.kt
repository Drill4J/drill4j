package com.epam.drill.plugins

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.AdminPluginPart
import java.io.File
import java.util.*
import kotlin.collections.HashSet


class Plugins {
    var plugins: MutableMap<String, DP> = HashMap()
    var pluginBeans: MutableMap<String, PluginBean> = HashMap()

    fun getBean(pluginId: String) = pluginBeans.get(pluginId)
}
typealias DP = Pair<AdminPluginPart, File>

val DP.serverInstance: AdminPluginPart
    get() = first
val DP.agentPluginPart: File
    get() = second