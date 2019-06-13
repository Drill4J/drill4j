package com.epam.drill.plugins

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.AdminPluginPart
import java.io.File
import java.util.*


class Plugins(val plugins: MutableMap<String, DP> = HashMap()) : MutableMap<String, DP> by plugins

typealias DP = Triple<Class<AdminPluginPart>, File, PluginBean>

val DP.pluginClass: Class<AdminPluginPart>
    get() = first
val DP.agentPluginPart: File
    get() = second
val DP.pluginBean: PluginBean
    get() = third

fun Plugins.getAllPluginBeans() = plugins.values.map { it.pluginBean }

infix fun PluginBean.partOf(set: List<String>?) =
    if (set == null) false else this.id in set