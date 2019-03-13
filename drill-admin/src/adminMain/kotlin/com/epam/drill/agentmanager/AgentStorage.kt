package com.epam.drill.agentmanager


//class AgentStorage(override val kodein: Kodein) : KodeinAware {
//    private val drillServerWs: DrillServerWs by instance()
//
//    val newAgents = ObservableMapStorage<AgentInfo, DefaultWebSocketSession>()
//
//
//    operator fun set(k: AgentInfo, v: DefaultWebSocketSession) {
//        newAgents[k] = v
//    }
//
//
//    suspend fun notifys() {
//
//        drillServerWs.sessionStorage["/agentStorages"]?.forEach {
//            val message = Gson().toJson((agents.values.map { it.agentInfo }))
//            val text = Gson().toJson(Message(MessageType.MESSAGE, "/agentStorages", message))
//            it.send(Frame.Text(text))
//        }
//
//    }
//
//    val agents: MutableMap<String, DrillAgent> = ConcurrentHashMap()
//
//    suspend fun addAgent(drillAgent: DrillAgent) {
//        agents[drillAgent.agentInfo.agentAddress] = drillAgent
//        notifys()
//        //fixme log
////        logDebug("Agent was added")
//    }
//
//    suspend fun removeAgent(name: String) {
//        agents.remove(name)
//        notifys()
//        //fixme log
////        logDebug("Agent was removed")
//    }
//
//}
