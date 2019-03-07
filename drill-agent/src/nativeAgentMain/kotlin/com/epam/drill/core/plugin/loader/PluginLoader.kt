package com.epam.drill.core.plugin.loader

import com.epam.drill.*
import com.epam.drill.core.exceptions.PluginLoadException
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import kotlinx.coroutines.runBlocking

val plLogger
    get() = DLogger("plLogger")

@ExperimentalUnsignedTypes
fun pluginLoadCommand() = runBlocking {
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
        PluginManager.addPlugin(NativePluginController(pluginFile))
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException ->
                plLogger.warn { "Can't load the plugin file ${pluginFile.absolutePath}. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of ${pluginFile.absolutePath} jar... Error: ${ex.message}" }
        }
    }
}

