package com.epam.drill.core.ws

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.core.agentInfo
import com.epam.drill.core.pluginLoadCommand
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager.pluginsState
import com.soywiz.korio.lang.Thread_sleep
import com.soywiz.korio.net.ws.WebSocketClient
import drillInternal.addMessage
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


fun startWs(): Future<Unit> {
    return Worker.start(true).execute(TransferMode.UNSAFE, {}) {
        initRuntimeIfNeeded()
        pluginLoadCommand()
        topicRegister()
        while (true) {
            wsLogger.debug { "create new Connection" }
            Thread_sleep(5000)
            try {
                websocket()
            } catch (sx: Throwable) {
                wsLogger.error { sx.message }
            }
        }
    }
}

private fun websocket() = runBlocking {
    launch {
        val url = "ws://${agentInfo.drillAdminUrl}/agent/attach"
        wsLogger.debug { "try to create websocket $url" }
        val wsClient = WebSocketClient(url)
        wsLogger.debug { "WS created" }

        wsClient.onAnyMessage.add { rawMessage ->
            println("[DEBUG] got message to the wsocket: $rawMessage")
        }
        wsClient.onStringMessage.add { rawMessage ->
            initRuntimeIfNeeded()
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
            if (destination != "/plugins/agent-attached")
                register()

        }
        wsClient.onBinaryMessage.add {
            val readBinary = readBinary(it)
            val rawMessage = readBinary.first
            val rawFileData = readBinary.second
            val message = rawMessage.stringFromUtf8().toWsMessage()
            val topic = WsRouter[message.destination]
            if (topic != null) {
                when (topic) {
                    is FileTopic -> topic.block(message.message, rawFileData)
                    else -> throw RuntimeException("We can't use any topics except FileTopic in binary retriever")
                }
            }
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

private fun String.toWsMessage() = Json().parse(Message.serializer(), this)

private fun register() = memScoped {
    try {
        agentInfo.rawPluginNames.clear()
        agentInfo.rawPluginNames.addAll(pluginsState())

        val message = Json.stringify(
            Message.serializer(),
            Message(MessageType.AGENT_REGISTER, message = Json.stringify(AgentInfo.serializer(), agentInfo))
        )

        addMessage(message.cstr.getPointer(Arena()))

    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}


fun readBinary(x: ByteArray): Pair<ByteArray, ByteArray> {
    val readInt = x.readInt(0)
    val indices = 8 until 8 + readInt
    println(indices)
    val message = x.sliceArray(indices)
    val indices1 = 8 + readInt until x.size
    println(indices1)
    val file = x.sliceArray(indices1)
    return message to file
}

private fun ByteArray.readInt(index: Int): Int {
    return (this[index].toInt() and 0xFF shl 24 or (this[index + 1].toInt() and 0xFF shl 16)
            or (this[index + 2].toInt() and 0xFF shl 8) or (this[index + 3].toInt() and 0xFF))
}