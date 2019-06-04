package com.epam.drill.core.ws

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.AgentIdParam
import com.epam.drill.common.DrillEvent
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.NeedSyncParam
import com.epam.drill.common.PluginMessage
import com.epam.drill.core.concurrency.LockFreeMPSCQueue
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.core.exceptions.WsClosedException
import com.epam.drill.core.exec
import com.epam.drill.core.needSync
import com.epam.drill.core.plugin.loader.loadPlugin
import com.epam.drill.core.util.json
import com.epam.drill.logger.DLogger
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.writeToFile
import com.soywiz.korio.net.ws.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.Cbor
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


val wsLogger
    get() = DLogger("DrillWebsocket")

@SharedImmutable
val wsThread = Worker.start(true)

@SharedImmutable
val sendWorker = Worker.start(true)

@SharedImmutable
val loader = Worker.start(true)

@ThreadLocal
private val guaranteeQueue = LockFreeMPSCQueue<String>()

fun sendMessage(message: String) {
    sendWorker.execute(TransferMode.UNSAFE, { message }) {
        guaranteeQueue.addLast(it)
    }
}


@ExperimentalUnsignedTypes
fun startWs() =
    wsThread.executeCoroutines {
        launch { topicRegister() }
        while (true) {
            delay(3000)
            try {
                runBlocking {
                    websocket(exec { adminAddress })
                }
            } catch (ex: Exception) {
//                when (ex) {
//                    is WsClosedException -> {
//                    }
                println(ex.message + "\ntry reconnect\n")
//                }
            }
        }
    }


suspend fun websocket(adminUrl: String) {
    val url = "ws://$adminUrl/agent/attach"
    wsLogger.debug { "try to create websocket $url" }
    val wsClient = WebSocketClient(
        url, params = mutableMapOf(
            AgentIdParam to exec { agentId },
            NeedSyncParam to needSync.toString()
        )
    )
    wsClient.onOpen {
        wsLogger.debug { "Agent connected" }
    }

    wsClient.onAnyMessage.add {
        //        wsLogger.debug { "got a message $rawMessage" }
        wsLogger.warn { "to" }
        sendMessage(kotlinx.serialization.json.Json.stringify(Message.serializer(), Message(MessageType.DEBUG, "", "")))
    }
    wsClient.onStringMessage.add { rawMessage ->
        val message = rawMessage.toWsMessage()
        val destination = message.destination
        val topic = WsRouter[destination]
        if (topic != null) {
            when (topic) {
                is FileTopic -> throw RuntimeException("We can't use File topic in not binary retriever")
                is InfoTopic -> topic.block(message.message)
                is GenericTopic<*> -> topic.deserializeAndRun(message.message)
            }
        } else {
            wsLogger.warn { "topic with name '$destination' didn't register" }
        }


    }
    wsClient.onBinaryMessage.add {
        val load = Cbor.load(PluginMessage.serializer(), it)
        when {
            load.event == DrillEvent.LOAD_PLUGIN -> {
                exec { pl[load.pluginName] = load.pl!! }
                loader.execute(TransferMode.UNSAFE, { load }) { plugMessage ->
                    runBlocking {
                        exec { needSync = false }
                        val pluginsDir = localVfs(drillInstallationDir)["drill-plugins"]
                        if (!pluginsDir.exists()) pluginsDir.mkdir()
                        val vfsFile = pluginsDir[plugMessage.pluginName]
                        if (!vfsFile.exists()) vfsFile.mkdir()
                        val plugin: DrillPluginFile = vfsFile["agent-part.jar"]
                        plugMessage.pluginFile.toByteArray().writeToFile(plugin)
                        loadPlugin(plugin)
                    }

                }
            }
            load.event == DrillEvent.SYNC_FINISHED -> {
                needSync = false
                wsLogger.info { "Agent synchronization is finished" }
            }
            load.event == DrillEvent.SYNC_STARTED -> wsLogger.info { "Agent synchronization is started" }
        }


    }
    wsClient.onError.add {
        wsLogger.error { "WS error: ${it.message}" }
    }
    wsClient.onClose.add {
        wsLogger.info { "Websocket closed" }
        wsClient.close()
        throw WsClosedException("")
    }

    coroutineScope {
        launch {
            while (true) {
                delay(50)
                val execute = sendWorker.execute(TransferMode.UNSAFE, {}) {
                    val first = guaranteeQueue.removeFirstOrNull()
                    first
                }.result
                if (execute != null) {
                    wsClient.send(execute)
//                    sendWorker.execute(TransferMode.UNSAFE, {}) {
//                        guaranteeQueue.removeFirstOrNull()
//                    }.result
                }
            }

        }
    }
}

private fun String.toWsMessage() = json.parse(Message.serializer(), this)


fun Worker.executeCoroutines(block: suspend CoroutineScope.() -> Unit): Future<Unit> {
    return this.execute(TransferMode.UNSAFE, { block }) {
        try {
            runBlocking {
                it(this)
            }
        } catch (ex: Throwable) {
            println("ss")
        }
    }
}