package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.Family
import com.epam.drill.core.exceptions.PluginLoadException
import com.epam.drill.core.exec
import com.epam.drill.jvmapi.AttachNativeThreadToJvm
import com.epam.drill.logger.DLogger
import com.epam.drill.pluginConfig

val plLogger
    get() = DLogger("plLogger")

suspend fun loadPlugin(pluginFile: DrillPluginFile) {
    AttachNativeThreadToJvm()
    pluginFile.addPluginsToSystemClassLoader()
    try {
        val pluginConfig = pluginFile.pluginConfig()
        when (pluginConfig.family) {
            Family.INSTRUMENTATION -> {
                InstrumentationNativePlugin(pluginFile).apply {
                    connect()
                    exec {
                        pstorage[this@apply.id] = this@apply
                    }
                    retransform()
                }
            }
            Family.GENERIC -> {
                GenericNativePlugin(pluginFile).apply {
                    connect()
                    exec {
                        pstorage[this@apply.id] = this@apply
                    }
                }
            }
        }
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException ->
                plLogger.warn { "Can't load the plugin file ${pluginFile.absolutePath}. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of ${pluginFile.absolutePath} jar... Error: ${ex.message}" }
        }
    }
}

