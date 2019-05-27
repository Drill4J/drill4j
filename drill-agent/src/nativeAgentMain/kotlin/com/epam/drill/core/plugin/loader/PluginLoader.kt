package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.Family
import com.epam.drill.core.exceptions.PluginLoadException
import com.epam.drill.jvmapi.AttachNativeThreadToJvm
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import com.epam.drill.pluginConfig
import kotlin.native.concurrent.Worker

val plLogger
    get() = DLogger("plLogger")

@SharedImmutable
val xx  = Worker.start(true);

@ExperimentalUnsignedTypes
suspend fun loadPlugin(pluginFile: DrillPluginFile) {
    AttachNativeThreadToJvm()
    pluginFile.addPluginsToSystemClassLoader()
    try {
        val pluginConfig = pluginFile.pluginConfig()
        when (pluginConfig.family) {
            Family.INSTRUMENTATION -> {
                val nativePluginController = InstrumentationNativePlugin(pluginFile).apply {
                    connect()
                    PluginManager.addPlugin(this)
                }
                nativePluginController.retransform()
            }
            Family.GENERIC -> {
                GenericNativePlugin(pluginFile).apply {
                    connect()
                    PluginManager.addPlugin(this)
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

