package com.epam.drill.endpoints

import com.epam.drill.agentmanager.*
import com.epam.drill.common.*
import com.epam.drill.dataclasses.*
import com.epam.drill.plugins.*
import com.epam.drill.service.*
import com.epam.drill.storage.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.cbor.*
import mu.*
import org.jetbrains.exposed.sql.*
import org.kodein.di.*
import org.kodein.di.generic.*

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
                AgentStatus.READY -> agentInfoDb.apply {
                    val existingVersion = buildVersions.find { it.buildVersion == pBuildVersion }
                    val buildVersion = existingVersion ?: AgentBuildVersion.new {
                        buildVersion = pBuildVersion
                        name = ""
                    }.apply { buildVersions = SizedCollection(buildVersions.toSet() + this) }

                    this.buildVersion = pBuildVersion
                    this.buildAlias = buildVersion.name
                }
                AgentStatus.NOT_REGISTERED -> {
                    //TODO: add some processing for unregistered agents
                }
                AgentStatus.DISABLED -> {
                    //TODO: add some processing for disabled agents
                }
                else -> Unit
            }
            agentInfoDb.toAgentInfo()
        } else {
            AgentInfoDb.new(agentId) {
                name = ""
                status = AgentStatus.NOT_REGISTERED
                groupName = ""
                description = ""
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
        get(agentId)?.apply {
            name = au.name
            groupName = au.group
            description = au.description
            buildAlias = au.buildVersions.firstOrNull { it.id == this.buildVersion }?.name!!
            buildVersions.replaceAll(au.buildVersions)
            status = au.status
            update(this@AgentManager)
        }

    }

    suspend fun updateAgentPluginConfig(agentId: String, pc: PluginConfig): Boolean = get(agentId)?.let { agentInfo ->
        agentInfo.plugins.find { it.id == pc.id }?.let { plugin ->
            if (plugin.config != pc.data) {
                plugin.config = pc.data
                agentInfo.update(this)
            }
        }
    } != null

    suspend fun resetAgent(agInfo: AgentInfo) {
        val au = AgentInfoWebSocketSingle(
            id = agInfo.id,
            name = "",
            group = "",
            status = AgentStatus.NOT_REGISTERED,
            description = "",
            buildVersion = agInfo.buildVersion,
            buildAlias = INITIAL_BUILD_ALIAS
        )
            .apply { buildVersions.add(AgentBuildVersionJson(agInfo.buildVersion, INITIAL_BUILD_ALIAS)) }
        updateAgent(agInfo.id, au)
    }

    suspend fun update() {
        agentStorage.update()
    }

    suspend fun singleUpdate(agentId: String) {
        agentStorage.singleUpdate(agentId)
    }

    suspend fun put(agentInfo: AgentInfo, session: DefaultWebSocketSession) {
        agentStorage.put(agentInfo.id, AgentEntry(agentInfo, session))
    }

    suspend fun remove(agentInfo: AgentInfo) {
        agentStorage.remove(agentInfo.id)
    }

    fun agentSession(k: String) = agentStorage.targetMap[k]?.agentSession

    operator fun contains(k: String) = k in agentStorage.targetMap

    operator fun get(agentId: String) = agentStorage.targetMap[agentId]?.agent
    fun full(agentId: String) = agentStorage.targetMap[agentId]

    fun getAllAgents() = agentStorage.targetMap.values

    fun getAllInstalledPluginBeanIds(agentId: String) = get(agentId)?.plugins?.map { it.id }

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

        val enabled = agentInfo.status == AgentStatus.READY

        val session = agentSession(agentInfo.id)
        session!!.send(
            Frame.Text(
                Message.serializer() stringify Message(MessageType.INFO, "", DrillEvent.SYNC_STARTED.name)
            )

        )
        agentInfo.plugins.forEach { pb ->
            val pluginId = pb.id
            plugins[pluginId]?.agentPluginPart?.let { agentPluginPart ->
                val pluginMessage =
                    PluginMessage(
                        DrillEvent.LOAD_PLUGIN,
                        agentPluginPart.readBytes().toList(),
                        if (plugins[pluginId]?.windowsPart != null || plugins[pluginId]?.linuxPar != null)
                            NativePlugin(
                                plugins[pluginId]?.windowsPart?.readBytes()?.toList() ?: emptyList(),
                                plugins[pluginId]?.linuxPar?.readBytes()?.toList() ?: emptyList()
                            ) else null,
                        pb.copy().apply { this.enabled = pb.enabled && enabled }
                    )

                session.send(Frame.Binary(false, Cbor.dump(PluginMessage.serializer(), pluginMessage)))
            }

            agentInfo.status = AgentStatus.BUSY
            update()
            singleUpdate(agentInfo.id)
            //fixme raw hack for pluginLoading.
            delay(10000)
        }


        session.send(
            Frame.Text(
                Message.serializer() stringify Message(MessageType.INFO, "", DrillEvent.SYNC_FINISHED.name)
            )
        )

    }

}
