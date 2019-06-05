package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.agentmanager.get
import com.epam.drill.agentmanager.self
import com.epam.drill.common.*
import com.epam.drill.dataclasses.AgentBuildVersion
import com.epam.drill.plugins.AgentPlugins
import com.epam.drill.service.asyncTransaction
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory

class AgentManager(override val kodein: Kodein) : KodeinAware {
    companion object {
        val logger = LoggerFactory.getLogger(AgentManager::class.java)
    }

    val agentStorage: AgentStorage by instance()
    val agentPlugins: AgentPlugins by instance()

    suspend fun agentConfiguration(agentId: String, pBuildVersion: String) = asyncTransaction {
        addLogger(StdOutSqlLogger)
        val agentInfoDb = AgentInfoDb.findById(agentId)
        if (agentInfoDb != null) {
            agentInfoDb.buildVersions.find { it.buildVersion == pBuildVersion } ?: run {
                agentInfoDb.buildVersions =
                        SizedCollection(AgentBuildVersion.new {
                            this.buildVersion = pBuildVersion
                            this.name = ""
                        })
            }

            agentInfoDb.toAgentInfo()
        } else {
            AgentInfoDb.new(agentId) {
                name = "???"
                groupName = "???"
                description = "???"
                this.buildVersion = pBuildVersion
                isEnable = true
                adminUrl = ""
                rawPluginNames = SizedCollection()

            }.apply {
                this.buildVersions =
                        SizedCollection(AgentBuildVersion.new {
                            this.buildVersion = pBuildVersion
                            this.name = ""
                        })
            }.toAgentInfo()


        }

    }


    suspend fun updateAgent(agentId: String, au: AgentInfoWebSocketSingle) {
        asyncTransaction {
            val agentInfoDb = AgentInfoDb.findById(agentId)
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

    operator fun contains(k: String) = k in agentStorage.targetMap

    fun self(k: String) = agentStorage.self(k)

    fun byId(agentId: String) = agentStorage.targetMap[agentId]?.first

    suspend fun addPluginFromLib(agentId: String, pluginId: String) = asyncTransaction {
        val agentInfoDb = AgentInfoDb.findById(agentId)
        if (agentInfoDb != null) {
            val plugin = agentPlugins.getBean(pluginId)!!
            val fillPluginBeanDb: PluginBeanDb.() -> Unit = {
                this.pluginId = plugin.id
                this.name = plugin.name
                this.description = plugin.description
                this.type = plugin.type
                this.family = plugin.family
                this.enabled = plugin.enabled
                this.config = plugin.config
            }
            val rawPluginNames = agentInfoDb.rawPluginNames.toList()
            val existingPluginBeanDb = rawPluginNames.find { it.pluginId == pluginId }
            if (existingPluginBeanDb == null) {
                val newPluginBeanDb = PluginBeanDb.new(fillPluginBeanDb)
                agentInfoDb.rawPluginNames = SizedCollection(rawPluginNames + newPluginBeanDb)
                newPluginBeanDb
            } else {
                existingPluginBeanDb.apply(fillPluginBeanDb)
            }
        } else {
            logger.warn("Agent with id $agentId not found in your DB.")
            null
        }
    }?.let { pluginBeanDb ->
        val agentInfo = byId(agentId)
        agentInfo!!.rawPluginNames.add(pluginBeanDb.toPluginBean())
        agentStorage.update()
        agentStorage.singleUpdate(agentId)
        updateAgentConfig(agentInfo)
        logger.info("Plugin $pluginId successfully added to agent with id $agentId!")
    }

    suspend fun updateAgentConfig(agentInfo: AgentInfo) {
        val session = agentStorage[agentInfo.id]
        session!!.send(
            Frame.Binary(
                false,
                Cbor.dump(PluginMessage.serializer(), PluginMessage(DrillEvent.SYNC_STARTED, ""))
            )
        )
        agentInfo.rawPluginNames.forEach { pb ->
            val pluginId = pb.id
            val agentPluginPart = agentPlugins.getAdminPart(pluginId)
            val pluginMessage =
                PluginMessage(
                    DrillEvent.LOAD_PLUGIN,
                    pluginId,
                    agentPluginPart.readBytes().toList(),
                    pb,
                    "-"
                )
            session.send(Frame.Binary(false, Cbor.dump(PluginMessage.serializer(), pluginMessage)))
        }


        session.send(
            Frame.Binary(
                false,
                Cbor.dump(PluginMessage.serializer(), PluginMessage(DrillEvent.SYNC_FINISHED, ""))
            )
        )
    }

}


@Serializable
data class AgentUpdate(
    val name: String,
    val groupName: String,
    val description: String,
    val buildVersion: String
)
