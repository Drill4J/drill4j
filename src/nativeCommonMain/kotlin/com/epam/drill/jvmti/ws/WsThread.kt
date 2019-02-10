package com.epam.drill.jvmti.ws

import com.epam.drill.common.AgentAdditionalInfo
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.jvmti.agentGroupName
import com.epam.drill.jvmti.agentName
import com.epam.drill.jvmti.drillAdminUrl
import com.epam.drill.jvmti.logger.DLogger
import com.soywiz.korio.lang.Thread_sleep
import com.soywiz.korio.net.ws.WebSocketClient
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


val wsLogger
    get() = DLogger("DrillWebsocket")


suspend fun register(wsClient: WebSocketClient) = memScoped {
    val agentInfo = AgentInfo(
        agentName, agentGroupName,
        mutableSetOf(),

        //todo remove this hardcode
        AgentAdditionalInfo(
            listOf(),
            4,
            "x64",
            "windows",
            "10",
            mapOf()
        )
    )

    val message = Json.stringify(
        Message.serializer(),
        Message(MessageType.AGENT_REGISTER, message = Json.stringify(AgentInfo.serializer(), agentInfo))
    )
    println(message)
    wsClient.send(message)

}

private fun ws() = runBlocking {
    launch {
        val url = "ws://$drillAdminUrl/agent/attach"
        wsLogger.debug { "try to create ws $url" }
        val wsClient = WebSocketClient(url)
        wsLogger.debug { "WS created" }
        wsClient.onAnyMessage.add {
            wsLogger.debug { "Any from socket: $it" }
        }

        wsClient.onStringMessage.add {
            wsLogger.debug { "String from socket: $it" }
        }
        wsClient.onError.add {
            wsLogger.error { "WS error: ${it.message}" }
        }
        wsClient.onClose.add {
            wsLogger.info { "Websocket closed" }
            //todo thing about this throwing...
            throw RuntimeException("close")
        }
        register(wsClient)
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


fun startWs(): Future<Unit> {
    val worker = Worker.start(true)
    return worker.execute(TransferMode.UNSAFE, {}) {
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
