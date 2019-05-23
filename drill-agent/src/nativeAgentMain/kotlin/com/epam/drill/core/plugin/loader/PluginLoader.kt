package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.core.exceptions.PluginLoadException
import com.epam.drill.core.exec
import com.epam.drill.iterateThroughPlugins
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import com.epam.drill.pluginConfig

val plLogger
    get() = DLogger("plLogger")

@ExperimentalUnsignedTypes
suspend fun pluginLoadCommand() {
    //init env...
    jvmapi.currentEnvs()
    iterateThroughPlugins { pluginFile ->
        loadPlugin(pluginFile)
    }
}

@ExperimentalUnsignedTypes
suspend fun loadPlugin(pluginFile: DrillPluginFile) {
    pluginFile.retrieveFacilitiesFromPlugin()
    pluginFile.addPluginsToSystemClassLoader()
    try {

        //fixme costyl for coverage plugin...
        val pluginConfig = pluginFile.pluginConfig()
        if (pluginConfig.id == "coverage") {
            println("coverage load as plugin")
            val nativePluginController = Instrumented(pluginFile).apply {
                connect()
            }
            exec {
                pInstrumentedStorage["coverage"] = nativePluginController
            }
        } else {
            PluginManager.addPlugin(NativePluginController(pluginFile).apply {
                connect()
            })
        }
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException ->
                plLogger.warn { "Can't load the plugin file ${pluginFile.absolutePath}. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of ${pluginFile.absolutePath} jar... Error: ${ex.message}" }
        }
    }
}

