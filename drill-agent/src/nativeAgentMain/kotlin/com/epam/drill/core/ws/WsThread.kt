package com.epam.drill.core.ws

import com.epam.drill.JarVfsFile
import com.epam.drill.common.*
import com.epam.drill.common.AgentInfo
import com.epam.drill.core.*
import com.epam.drill.extractPluginFacilitiesTo
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.PluginManager.pluginsState
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.writeToFile
import com.soywiz.korio.lang.Thread_sleep
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.util.OS
import drillInternal.addMessage
import jvmapi.AddToSystemClassLoaderSearch
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


val wsLogger
    get() = DLogger("DrillWebsocket")


fun register() = memScoped {
    try {

        val rawPluginNames = pluginsState()

        val agentInfo = AgentInfo(
            agentName,
            "127.0.0.1", // - ipconfig or ifconfig. depends on Platform
            agentGroupName,
            agentDescription,
            rawPluginNames,

            AgentAdditionalInfo(
                listOf(),
                4,
                "x64",
                OS.platformNameLC + ":" + OS.platformName,
                "10",
                mapOf()
            )
        )


        val message = Json.stringify(
            Message.serializer(),
            Message(MessageType.AGENT_REGISTER, message = Json.stringify(AgentInfo.serializer(), agentInfo))
        )

        addMessage(message.cstr.getPointer(Arena()))

    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}

fun ws() = runBlocking {
    launch {
        val url = "ws://$drillAdminUrl/agent/attach"
        wsLogger.debug { "try to create ws $url" }
        val wsClient = WebSocketClient(url)
        wsLogger.debug { "WS created" }

        wsClient.onAnyMessage.add { rawMessage ->

        }
        wsClient.onStringMessage.add { rawMessage ->
            initRuntimeIfNeeded()
            val message = Json().parse(Message.serializer(), rawMessage)
            if (message.destination == "/") {
                val action = Json().parse(AgentEvent.serializer(), message.message)
                when (action.event) {
                    DrillEvent.UNLOAD_PLUGIN -> {
                        runBlocking {
                            unload(action.data)
                            register()
                        }
                    }
                    DrillEvent.AGENT_LOAD_SUCCESSFULLY -> wsLogger.info { "connected with DrillConsole" }
                    DrillEvent.LOAD_PLUGIN -> wsLogger.info { "'${DrillEvent.LOAD_PLUGIN}' event waiting for a file" }
                }
            } else if (message.destination.startsWith("updatePluginConfig")) {

                val pluginName = message.destination.split("/")[1]

                val agentPluginPart = PluginManager[pluginName]

                agentPluginPart?.updateRawConfig(message.message)
            }


        }
        wsClient.onBinaryMessage.add {
            load(it)

            register()


        }
        wsClient.onError.add {
            wsLogger.error { "WS error: ${it.message}" }
        }
        wsClient.onClose.add {
            wsLogger.info { "Websocket closed" }
            //todo thing about this throwing...
            throw RuntimeException("close")
        }
        register()
        launch {
            while (true) {
                delay(5)
                MessageQueue.retrieveMessage()?.apply {
                    wsClient.send(this)
                }

            }
        }
    }.join()
}


private suspend fun unload(pluginName: String) {
    PluginManager[pluginName]?.unload()
}

fun load(it: ByteArray) = runBlocking {
    try {
        val (pluginName, rawFileData) = parseRawPluginFromAdminStorage(it)
        val pluginsDir = localVfs(drillInstallationDir)["drill-plugins"]
        if (!pluginsDir.exists()) {
            pluginsDir.mkdir()
        }
        val vfsFile = pluginsDir[pluginName]
        if (!vfsFile.exists()) {
            vfsFile.mkdir()
        }
        val targetFile: JarVfsFile = vfsFile["agent-part.jar"]
        rawFileData.writeToFile(targetFile)
        try {
            targetFile.extractPluginFacilitiesTo(localVfs(targetFile.parent.absolutePath)) { vf ->
                !vf.baseName.contains("nativePart") &&
                        !vf.baseName.contains("static")
            }
            //init env...
            com.epam.drill.jvmapi.currentEnvs()

            AddToSystemClassLoaderSearch(targetFile.absolutePath)


            loadPlugin(targetFile)


            wsLogger.info { "load $pluginName" }
        } catch (ex: Exception) {
            wsLogger.error { "cant load the plugin..." }
            ex.printStackTrace()
        }

    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}

fun parseRawPluginFromAdminStorage(it: ByteArray): Pair<String, ByteArray> {
    val pluginName = it.sliceArray(0 until 20).stringFromUtf8().replace("!", "")
    val rawFileData = it.sliceArray(20 until it.size)
    return Pair(pluginName, rawFileData)
}

fun startWs(): Future<Unit> {
    val worker = Worker.start(true)
    return worker.execute(TransferMode.UNSAFE, {}) {
        initRuntimeIfNeeded()
        pluginLoadCommand()
        while (true) {
            wsLogger.debug { "create new Connection" }
            Thread_sleep(5000)
            try {
                ws()
            } catch (sx: Throwable) {
                wsLogger.error { sx.message }
            }
        }
    }
}

