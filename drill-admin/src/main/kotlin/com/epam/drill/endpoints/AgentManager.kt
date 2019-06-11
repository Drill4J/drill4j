package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.common.*
import com.epam.drill.dataclasses.AgentBuildVersion
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.agentPluginPart
import com.epam.drill.plugins.pluginBean
import com.epam.drill.service.asyncTransaction
import com.epam.drill.storage.AgentStorage
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.serialization.cbor.Cbor
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

private val logger = KotlinLogging.logger {}

const val INITIAL_BUILD_ALIAS = "Initial build"

class AgentManager(override val kodein: Kodein) : KodeinAware {

    val agentStorage: AgentStorage by instance()
    val plugins: Plugins by instance()

    suspend fun agentConfiguration(agentId: String, pBuildVersion: String) = asyncTransaction {
        addLogger(StdOutSqlLogger)
        val agentInfoDb = AgentInfoDb.findById(agentId)
        if (agentInfoDb != null) {
            when (agentInfoDb.status) {
                AgentStatus.READY -> {
                    agentInfoDb.buildVersions.find { it.buildVersion == pBuildVersion } ?: run {
                        agentInfoDb.buildVersions =
                            SizedCollection(AgentBuildVersion.new {
                                this.buildVersion = pBuildVersion
                                this.name = ""
                            })
                    }
                }
                AgentStatus.NOT_REGISTERED -> {
                    //TODO: add some processing for unregistered agents
                }
                AgentStatus.DISABLED -> {
                    //TODO: add some processing for disabled agents
                }
            }
            agentInfoDb.toAgentInfo()
        } else {
            AgentInfoDb.new(agentId) {
                name = "-"
                status = AgentStatus.NOT_REGISTERED
                groupName = "-"
                description = "-"
                this.buildVersion = pBuildVersion
                buildAlias = INITIAL_BUILD_ALIAS
                adminUrl = ""
                plugins = SizedCollection()

            }.apply {
                this.buildVersions =
                    SizedCollection(AgentBuildVersion.new {
                        this.buildVersion = pBuildVersion
                        this.name = INITIAL_BUILD_ALIAS
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
        val byId = get(agentId)
        byId?.apply {
            name = au.name
            groupName = au.group
            description = au.description
            buildVersion = au.buildVersion
            buildAlias = au.buildAlias
            status = au.status
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

    fun agentSession(k: String) = agentStorage.targetMap[k]?.second

    operator fun contains(k: String) = k in agentStorage.targetMap

    operator fun get(agentId: String) = agentStorage.targetMap[agentId]?.first

    suspend fun addPluginFromLib(agentId: String, pluginId: String) = asyncTransaction {
        val agentInfoDb = AgentInfoDb.findById(agentId)
        if (agentInfoDb != null) {
            plugins[pluginId]?.pluginBean?.let { plugin ->
                val fillPluginBeanDb: PluginBeanDb.() -> Unit = {
                    this.pluginId = plugin.id
                    this.name = plugin.name
                    this.description = plugin.description
                    this.type = plugin.type
                    this.family = plugin.family
                    this.enabled = plugin.enabled
                    this.config = plugin.config
                }
                val rawPluginNames = agentInfoDb.plugins.toList()
                val existingPluginBeanDb = rawPluginNames.find { it.pluginId == pluginId }
                if (existingPluginBeanDb == null) {
                    val newPluginBeanDb = PluginBeanDb.new(fillPluginBeanDb)
                    agentInfoDb.plugins = SizedCollection(rawPluginNames + newPluginBeanDb)
                    newPluginBeanDb
                } else {
                    existingPluginBeanDb.apply(fillPluginBeanDb)
                }
            }
        } else {
            logger.warn { "Agent with id $agentId not found in your DB." }
            null
        }
    }?.let { pluginBeanDb ->
        val agentInfo = get(agentId)
        agentInfo!!.plugins.add(pluginBeanDb.toPluginBean())
        agentStorage.update()
        agentStorage.singleUpdate(agentId)
        updateAgentConfig(agentInfo)
        logger.info { "Plugin $pluginId successfully added to agent with id $agentId!" }
    }

    suspend fun updateAgentConfig(agentInfo: AgentInfo) {
        val session = agentSession(agentInfo.id)
        session!!.send(
            Frame.Binary(
                false,
                Cbor.dump(PluginMessage.serializer(), PluginMessage(DrillEvent.SYNC_STARTED, ""))
            )
        )
        agentInfo.plugins.forEach { pb ->
            val pluginId = pb.id
            plugins[pluginId]?.agentPluginPart?.let { agentPluginPart ->
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
        }


        session.send(
            Frame.Binary(
                false,
                Cbor.dump(PluginMessage.serializer(), PluginMessage(DrillEvent.SYNC_FINISHED, ""))
            )
        )
    }

}
