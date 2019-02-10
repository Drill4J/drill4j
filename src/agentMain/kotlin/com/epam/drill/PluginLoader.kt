package com.epam.drill

import com.epam.drill.common.dynamicloader.extractConfigFile
import com.epam.drill.common.dynamicloader.loadInRuntime
import com.epam.drill.common.dynamicloader.retrieveApiClass
import com.epam.drill.plugin.api.SendListener
import com.epam.drill.plugin.api.processing.AgentPluginPart
import com.epam.drill.plugin.api.processing.DrillConstants
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.ws.Ws
import java.io.File
import java.util.jar.JarFile

object PluginLoader {
    val loadedPluginsHandler = HashMap<String, AgentPluginPart>()
    fun register() {
        DrillConstants.welcomeAdv()
//        Sender.listener = SendListener()

        val file = DrillConstants.DRILL_HOME
        if (!file.exists()) {

        } else {
            file.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory) {
                    val value = File(pluginDir, "agent-part.jar")
                    println("try load")
                    PluginLoader.loadPluginInRuntime(value)

                }
            }
        }
//        run()
    }

    fun loadPluginInRuntime(path: String) {
        PluginLoader.loadPluginInRuntime(File(path))
    }

    fun loadPluginInRuntime(f: File) {
        if (f.exists()) {
            val jarFile = JarFile(f)
            val entrySet = jarFile.entries().iterator().asSequence().toSet()
            extractConfigFile(jarFile, f.parentFile)

            val retrieveApiClass = retrieveApiClass(AgentPluginPart::class.java, entrySet)
            val agentPluginPart = retrieveApiClass.newInstance() as AgentPluginPart

            loadedPluginsHandler[agentPluginPart.pluginInfo().id] = agentPluginPart
//            Ws.subscribe(agentPluginPart.configClass, "updatePluginConfig/${agentPluginPart.pluginInfo().id}") {
//                agentPluginPart.updateConfig(it)
//                logDebug("Plugin config was updated. Plugin name ${it.id}")
//            }

        }
    }


}
