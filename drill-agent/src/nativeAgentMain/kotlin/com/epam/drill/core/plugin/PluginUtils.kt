package com.epam.drill.core.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.core.util.json
import com.epam.drill.logger.DLogger
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localVfs

val puLogger
    get() = DLogger("PluginUtils")


fun configFile(pluginId: String): VfsFile {
    return localVfs(drillInstallationDir)["drill-plugins"][pluginId]["static"]["plugin_config.json"]
}


suspend fun rawPluginConfigById(pluginId: String): String {
    return configFile(pluginId).readString()
}

suspend fun pluginConfigById(pluginId: String) = json.parse(PluginBean.serializer(), rawPluginConfigById(pluginId))


suspend fun PluginBean.dumpConfigToFileSystem() {
    configFile(this@dumpConfigToFileSystem.id).writeString(
        json.stringify(
            PluginBean.serializer(),
            this@dumpConfigToFileSystem
        )
    )
    puLogger.warn { "Config for plugin '${this@dumpConfigToFileSystem.id}' saved to file system" }
}