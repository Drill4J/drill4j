package com.epam.drill.plugin.api.end

import com.epam.drill.common.AgentInfo

interface WsService {
    suspend fun convertAndSend(agentInfo: AgentInfo, destination: String, message: String)
    fun getPlWsSession(): Set<String>
    fun storeData(key: String, obj: Any)
    fun retrieveData(key: String): Any?
    fun getEntityBy(agentId: String, clazz: Class<Any>)
}
