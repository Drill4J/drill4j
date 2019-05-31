package com.epam.drill.plugin.api.end

import com.epam.drill.common.AgentInfo

interface WsService {
    suspend fun convertAndSend(agentInfo: AgentInfo, destination: String, message: String)
    fun getPlWsSession(): Set<String>
    fun storeData(agentId: String, obj: Any)
    fun getEntityBy(agentId: String, clazz: Class<Any>)
}
