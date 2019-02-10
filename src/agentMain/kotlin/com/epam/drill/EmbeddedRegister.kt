package com.epam.drill

import com.epam.drill.common.AgentEvent
import com.epam.drill.common.DrillEvent
import com.epam.drill.plugin.api.SendListener
import com.epam.drill.plugin.api.processing.DrillConstants
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.ws.Ws
import com.soywiz.klogger.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


object DrillRegister {
    private val logger = Logger("Register")

    fun register() {
        DrillConstants.welcomeAdv()
        Sender.listener = SendListener()

        val file = DrillConstants.DRILL_HOME
        if (!file.exists()) {
            logger.warn { "There aren't any plugins... where is base dir?" }
        } else {
            file.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory) {
                    val value = File(pluginDir, "agent-part.jar")
                    logger.warn { "try to load ${pluginDir.name} plugin by ${value.absolutePath}" }
                    PluginLoader.loadPluginInRuntime(value)

                }
            }
        }
//        run()
    }

    private fun run() {
        Ws.subscribe<AgentEvent>("/") { action ->
            when (action.event) {
                DrillEvent.UNLOAD_PLUGIN -> unload(action.data)
                DrillEvent.AGENT_LOAD_SUCCESSFULLY -> logger.info { "connected with DrillConsole" }
                DrillEvent.LOAD_PLUGIN -> logger.info { "'${DrillEvent.LOAD_PLUGIN}' event waiting for a file" }

            }
        }

        Ws.binaryRetriever {
            load(it)
        }
    }


    private fun unload(pluginName: String) {
        val fileDir = File(DrillConstants.DRILL_HOME, pluginName)
        val file = File(fileDir, "agent-part.jar")
        file.delete()
        logger.info { "unload $pluginName" }
        PluginLoader.loadedPluginsHandler[pluginName]?.tryUnload()
    }

    private fun load(it: ByteBuffer) {
        val append = false
        val tempFile = File("agent-part.jar")
        var s = ""
        for (i in 0 until 20) {
            s += String(arrayOf(it.get()).toByteArray())
        }
        s = s.replace("!", "")
        val wChannel = FileOutputStream(tempFile, append).channel
        wChannel.write(it)
        wChannel.close()
        try {
            val target = File(File(DrillConstants.DRILL_HOME, s), "agent-part.jar")
            tempFile.copyTo(target, true)
            logger.info { "load $s" }
            GlobalScope.launch {
                PluginLoader.loadPluginInRuntime(target)
            }
        } catch (ex: Exception) {
            logger.error { "cant load the plugin..." }
            ex.printStackTrace()
        }

    }


}




