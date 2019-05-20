package com.epam.drill.core.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.logger.DLogger
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localVfs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

val puLogger
    get() = DLogger("PluginUtils")


fun configFile(pluginId: String): VfsFile {
    return localVfs(drillInstallationDir)["drill-plugins"][pluginId]["static"]["plugin_config.json"]
}


suspend fun rawPluginConfigById(pluginId: String): String {
    return configFile(pluginId).readString()
}

fun pluginConfigById(pluginId: String) = runBlocking {
    Json().parse(PluginBean.serializer(), rawPluginConfigById(pluginId))
}

fun PluginBean.dumpConfigToFileSystem() = runBlocking {
    configFile(this@dumpConfigToFileSystem.id).writeString(
        Json().stringify(
            PluginBean.serializer(),
            this@dumpConfigToFileSystem
        )
    )
    puLogger.warn { "Config for plugin '${this@dumpConfigToFileSystem.id}' saved to file system" }
}