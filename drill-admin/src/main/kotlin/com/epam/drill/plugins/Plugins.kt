package com.epam.drill.plugins

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.AdminPluginPart
import java.io.File
import java.util.*


class Plugins(val plugins: MutableMap<String, DP> = HashMap()) : MutableMap<String, DP> by plugins

typealias DP = Triple<Class<AdminPluginPart>, Triple<File, File?, File?>, PluginBean>

val DP.pluginClass: Class<AdminPluginPart>
    get() = first
val DP.agentPluginPart: File
    get() = second.first
val DP.windowsPart: File?
    get() = second.second
val DP.linuxPar: File?
    get() = second.third
val DP.pluginBean: PluginBean
    get() = third