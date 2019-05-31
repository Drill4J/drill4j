package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.AgentInfoDb
import com.epam.drill.common.merge
import com.epam.drill.common.toAgentInfo
import com.epam.drill.storage.CassandraConnector
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import kotlinx.serialization.Serializable
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

class AgentManager(override val kodein: Kodein) : KodeinAware {
    private val cc: CassandraConnector by instance()
    val agentStorage: AgentStorage by instance()

    fun agentConfiguration(agentId: String): AgentInfo {
        val agentInfo = cc.addEntityManager(agentId).find(AgentInfoDb::class.java, agentId)
        if (agentInfo != null)
            return agentInfo.toAgentInfo()
        else {
            return AgentInfoDb(
                id = agentId,
                name = "???",
                groupName = "???",
                description = "???",
                ipAddress = "???",
                buildVersion = "???",
                isEnable = true,
                adminUrl = "",
                rawPluginNames = mutableSetOf()
            ).apply {
                cc.getEntityManagerByKeyspace(agentId).persist(this)
            }.toAgentInfo()
        }

    }


    suspend fun updateAgent(agentId: String, au: AgentUpdate) {
        val manager = cc.addEntityManager(agentId)
        val agentInfoDb = manager.find(AgentInfoDb::class.java, agentId)
        agentInfoDb.merge(au)
        manager.persist(agentInfoDb)
        val byId = byId(agentId)
        byId?.apply {
            name = au.name
            groupName = au.groupName
            description = au.description
            buildVersion = au.buildVersion
        }
        agentStorage.update()
    }

    suspend fun put(agentInfo: AgentInfo, session: DefaultWebSocketSession) {
        agentStorage.put(agentInfo.id, Pair(agentInfo, session))
    }

    suspend fun remove(agentInfo: AgentInfo) {
        agentStorage.remove(agentInfo.id)
    }

    operator fun get(k: String) = agentStorage.targetMap[k]?.second

    fun self(k: String) = agentStorage.targetMap[k]?.first

    fun byId(agentId: String) = agentStorage.targetMap[agentId]?.first


}


@Serializable
data class AgentUpdate(
    val name: String,
    val groupName: String,
    val description: String,
    val buildVersion: String
)
