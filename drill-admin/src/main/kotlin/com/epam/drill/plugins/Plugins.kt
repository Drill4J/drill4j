package com.epam.drill.plugins

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.AdminPluginPart
import java.io.File
import java.util.*
import kotlin.collections.HashSet


class Plugins {
    val plugins: MutableMap<String, DP> = HashMap()
    val pluginBeans: MutableMap<String, PluginBean> = HashMap()

    operator fun get(pluginId: String) = pluginBeans[pluginId]

    operator fun contains(pluginId: String) = pluginId in pluginBeans
}
typealias DP = Pair<AdminPluginPart, File>

val DP.serverInstance: AdminPluginPart
    get() = first
val DP.agentPluginPart: File
    get() = second