package com.epam.drill.plugins

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.AdminPluginPart
import java.io.File
import java.util.*


class Plugins(val plugins: MutableMap<String, DP> = HashMap()) : MutableMap<String, DP> by plugins

typealias DP = Triple<AdminPluginPart, File, PluginBean>

val DP.serverInstance: AdminPluginPart
    get() = first
val DP.agentPluginPart: File
    get() = second
val DP.pluginBean: PluginBean
    get() = third