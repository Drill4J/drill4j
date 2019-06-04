package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.agentmanager.self
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.AgentInfoDb
import com.epam.drill.common.PluginBeanDb
import com.epam.drill.common.merge
import com.epam.drill.common.toAgentInfo
import com.epam.drill.plugins.AgentPlugins
import com.epam.drill.service.asyncTransaction
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

class AgentManager(override val kodein: Kodein) : KodeinAware {
    val agentStorage: AgentStorage by instance()
    val agentPlugins: AgentPlugins by instance()

    suspend fun agentConfiguration(agentId: String): AgentInfo {
        val agentInfo = asyncTransaction { AgentInfoDb.findById(agentId) }
        if (agentInfo != null)
            return asyncTransaction {
                agentInfo.toAgentInfo()
            }
        else {
            return asyncTransaction {
                addLogger(StdOutSqlLogger)
                AgentInfoDb.new(agentId) {
                    name = "???"
                    groupName = "???"
                    description = "???"
                    buildVersion = "???"
                    isEnable = true
                    adminUrl = ""
                    rawPluginNames = SizedCollection(emptyList())
                }
                    .toAgentInfo()
            }

        }

    }


    suspend fun updateAgent(agentId: String, au: AgentInfoWebSocketSingle) {
        val agentInfoDb = asyncTransaction { AgentInfoDb.findById(agentId) }

        asyncTransaction {
            addLogger(StdOutSqlLogger)
            agentInfoDb?.merge(au)
        }
        val byId = byId(agentId)
        byId?.apply {
            name = au.name
            groupName = au.group
            description = au.description
            buildVersion = au.buildVersion
        }
        agentStorage.update()
        agentStorage.singleUpdate(agentId)

    }

    suspend fun put(agentInfo: AgentInfo, session: DefaultWebSocketSession) {
        agentStorage.put(agentInfo.id, Pair(agentInfo, session))
    }

    suspend fun remove(agentInfo: AgentInfo) {
        agentStorage.remove(agentInfo.id)
    }

    operator fun get(k: String) = agentStorage.targetMap[k]?.second

    fun self(k: String) = agentStorage.self(k)

    fun byId(agentId: String) = agentStorage.targetMap[agentId]?.first

    fun addPluginFromLib(agentId: String, pluginId: String): AgentInfo? {
        val agentInfoDb = AgentInfoDb.findById(agentId)
        if(!(agentInfoDb == null)) {
            val col = agentInfoDb?.rawPluginNames?.toMutableSet()
            val plugin = agentPlugins.getBean(pluginId)
            val dbBean = PluginBeanDb.new {
                this.pluginId = plugin!!.id
                this.name = plugin.name
                this.description = plugin.description
                this.type = plugin.type
                this.family = plugin.family
                this.enabled = plugin.enabled
                this.config = plugin.config
            }
            col!!.add(dbBean)
            val rawNames = SizedCollection(col)
            agentInfoDb.rawPluginNames = rawNames
            println("Plugin with id $pluginId have successfully been added to agent with id $agentId!")
            return agentInfoDb.toAgentInfo()
        }
        else{
            println("Agent with id $agentId have not been found on your DB.")
            return null
        }
    }

}


@Serializable
data class AgentUpdate(
    val name: String,
    val groupName: String,
    val description: String,
    val buildVersion: String
)
