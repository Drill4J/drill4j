package com.epam.drill.core.ws

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.core.agentInfo
import com.epam.drill.core.concurrency.LockFreeMPSCQueue
import com.epam.drill.core.plugin.loader.pluginLoadCommand
import com.epam.drill.core.util.json
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager.pluginsState
import com.soywiz.korio.net.ws.WebSocketClient
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


val wsLogger
    get() = DLogger("DrillWebsocket")

@SharedImmutable
val wsThread = Worker.start(true)

@SharedImmutable
val sendWorker = Worker.start(true)

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
        pluginLoadCommand()
        topicRegister()
        while (true)
            try {
                runBlocking {
                    delay(3000)
                    websocket(agentInfo.adminUrl)
                }
            } catch (ex: Exception) {
                println(ex.message + "\ntry reconnect\n")
            }
    }


suspend fun websocket(adminUrl: String) {
    val url = "ws://$adminUrl/agent/attach"
    wsLogger.debug { "try to create websocket $url" }
    val wsClient = WebSocketClient(url)
    wsClient.onOpen {
        wsLogger.debug { "WS created" }
    }

    wsClient.onAnyMessage.add { rawMessage ->
        wsLogger.debug { "got a message $rawMessage" }
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
        delay(50)
        register()

    }
    wsClient.onError.add {
        wsLogger.error { "WS error: ${it.message}" }
    }
    wsClient.onClose.add {
        wsLogger.info { "Websocket closed" }
        wsClient.close()
    }


    agentInfo.rawPluginNames.clear()
    agentInfo.rawPluginNames.addAll(pluginsState())

    val message = json.stringify(
        Message.serializer(),
        Message(MessageType.AGENT_REGISTER, message = json.stringify(AgentInfo.serializer(), agentInfo))
    )
    wsClient.send(message)
    delay(2000)
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

private suspend fun register() = memScoped {
    try {
        agentInfo.rawPluginNames.clear()
        agentInfo.rawPluginNames.addAll(pluginsState())

        val message = json.stringify(
            Message.serializer(),
            Message(MessageType.AGENT_REGISTER, message = json.stringify(AgentInfo.serializer(), agentInfo))
        )
        sendMessage(message)
    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}


fun readBinary(x: ByteArray): Pair<ByteArray, ByteArray> {
    val readInt = x.readInt(0)
    val indices = 8 until 8 + readInt
    val message = x.sliceArray(indices)
    val file = x.sliceArray(8 + readInt until x.size)
    return message to file
}

private fun ByteArray.readInt(index: Int): Int {
    return (this[index].toInt() and 0xFF shl 24 or (this[index + 1].toInt() and 0xFF shl 16)
            or (this[index + 2].toInt() and 0xFF shl 8) or (this[index + 3].toInt() and 0xFF))
}


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