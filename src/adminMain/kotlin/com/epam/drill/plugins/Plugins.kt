package com.epam.drill.plugins

import com.epam.drill.plugin.api.end.AdminPluginPart
import java.io.File
import java.util.*


class Plugins {
    var plugins: MutableMap<String, DP> = HashMap()

}
typealias DP = Pair<AdminPluginPart, File>

val DP.serverInstance: AdminPluginPart
    get() = first
val DP.agentPluginPart: File
    get() = second