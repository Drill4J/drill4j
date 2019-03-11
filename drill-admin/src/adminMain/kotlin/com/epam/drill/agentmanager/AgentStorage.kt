package com.epam.drill.agentmanager

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.endpoints.DrillServerWs
import com.google.gson.Gson
import io.ktor.http.cio.websocket.Frame
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.util.concurrent.ConcurrentHashMap


class AgentStorage(override val kodein: Kodein) : KodeinAware {
    private val drillServerWs: DrillServerWs by instance()

    suspend fun notifys() {

        drillServerWs.sessionStorage["/agentStorages"]?.forEach {
            val message = Gson().toJson((agents.values.map { it.agentInfo }))
            val text = Gson().toJson(Message(MessageType.MESSAGE, "/agentStorages", message))
            it.send(Frame.Text(text))
        }

    }

    val agents: MutableMap<String, DrillAgent> = ConcurrentHashMap()

    suspend fun addAgent(drillAgent: DrillAgent) {
        agents[drillAgent.agentInfo.agentAddress] = drillAgent
        notifys()
        //fixme log
//        logDebug("Agent was added")
    }

    suspend fun removeAgent(name: String) {
        agents.remove(name)
        notifys()
        //fixme log
//        logDebug("Agent was removed")
    }

}
