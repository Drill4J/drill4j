package com.epam.drill.core.ws

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import com.epam.drill.core.agentInfo
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.core.plugin.loader.loadPlugin
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.api.processing.Reason
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.file.writeToFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.ThreadLocal


@ExperimentalUnsignedTypes
fun topicRegister() =
    WsRouter {
        topic("/plugins/load").withPluginFileTopic { pluginId, plugin ->
            wsLogger.warn { "Load event. Plugin id is $pluginId" }
            if (PluginManager[pluginId] != null) {
                wsLogger.warn { "plugin '$pluginId' already is loaded" }
            } else
                loadPlugin(plugin)
        }

        topic("/plugins/unload").rawMessage { pluginId ->
            wsLogger.warn { "Unload event. Plugin id is $pluginId" }
            PluginManager[pluginId]?.unload(Reason.ACTION_FROM_ADMIN)
            //fixme physical deletion
        }


        topic("/plugins/agent-attached").rawMessage {
            wsLogger.warn { "Agent is attached" }
        }

        topic("/plugins/updatePluginConfig").withGenericTopic(PluginBean.serializer()) { config ->
            wsLogger.warn { "updatePluginConfig event: message is $config " }
            val agentPluginPart = PluginManager[config.id]
            if (agentPluginPart != null) {
                agentPluginPart.updateRawConfig(config)
                agentPluginPart.np?.updateRawConfig(config)
                val vfsFile =
                    localVfs(drillInstallationDir)["drill-plugins"][config.id]["static"]["plugin_config.json"]
                vfsFile.writeString(Json().stringify(PluginBean.serializer(), config))
                wsLogger.warn { "new settings for ${config.id} was save to file" }
            } else
                wsLogger.warn { "Plugin ${config.id} not loaded to agent" }

        }


        topic("/plugins/togglePlugin").rawMessage { pluginId ->
            wsLogger.warn { "togglePlugin event: PluginId is $pluginId" }
            PluginManager[pluginId]?.on()
            val vfsFile = localVfs(drillInstallationDir)["drill-plugins"][pluginId]["static"]["plugin_config.json"]
            val content =
                vfsFile.readString()
            val data = Json().parse(PluginBean.serializer(), content)
            val agentPluginPart = PluginManager[pluginId]
            if (agentPluginPart != null) {
                if (agentPluginPart.enabled) {
                    agentPluginPart.off()
                    data.enabled = false
                } else {
                    agentPluginPart.on()
                    data.enabled = true
                }
                vfsFile.writeString(Json().stringify(PluginBean.serializer(), data))
            } else
                wsLogger.warn { "Plugin $pluginId not loaded to agent" }

        }

        topic("/agent/updateAgentConfig").withGenericTopic(AgentInfo.serializer()) { info ->
            wsLogger.warn { "updateAgentConfig event: Info is $info" }
            agentInfo = info
            val data = Json().stringify(
                AgentInfo.serializer(),
                agentInfo
            )
            resourcesVfs["$drillInstallationDir/configs/drillConfig.json"].writeString(data)
            println(data)
        }

        topic("agent/toggleStandBy").rawMessage {
            wsLogger.warn { "toggleStandBy event" }
            if (agentInfo.isEnable) {
                agentInfo.rawPluginNames.forEach {
                    PluginManager[it.id]?.off()
                }
                agentInfo.isEnable = false
            } else {
                agentInfo.rawPluginNames.forEach {
                    //todo load state from cofig file
                    PluginManager[it.id]?.on()
                }
                agentInfo.isEnable = true
            }
        }
    }


@ThreadLocal
object WsRouter {

    val mapper = mutableMapOf<String, Topic>()
    operator fun invoke(alotoftopics: WsRouter.() -> Unit) {
        alotoftopics(this)
    }


    @Suppress("ClassName")
    open class inners(open val destination: String) {
        fun <T> withGenericTopic(des: DeserializationStrategy<T>, block: suspend (T) -> Unit): GenericTopic<T> {
            val genericTopic = GenericTopic(destination, des, block)
            mapper[destination] = genericTopic
            return genericTopic
        }


        @Suppress("unused")
        fun withFileTopic(xx: (message: String, file: ByteArray) -> Unit): FileTopic {
            val fileTopic = FileTopic(destination, xx)
            mapper[destination] = fileTopic
            return fileTopic
        }


        fun rawMessage(xx: suspend (String) -> Unit): InfoTopic {
            val infoTopic = InfoTopic(destination, xx)
            mapper[destination] = infoTopic
            return infoTopic
        }


    }

    operator fun get(topic: String): Topic? {
        return mapper[topic]
    }

}

@Suppress("unused")
fun WsRouter.topic(url: String): WsRouter.inners {
    return WsRouter.inners(url)
}

fun WsRouter.inners.withPluginFileTopic(bl: suspend (message: String, plugin: DrillPluginFile) -> Unit): FileTopic {
    val fileTopic = PluginTopic(destination) { pluginId, file ->
        runBlocking {
            val pluginsDir = localVfs(drillInstallationDir)["drill-plugins"]
            if (!pluginsDir.exists()) pluginsDir.mkdir()
            val vfsFile = pluginsDir[pluginId]
            if (!vfsFile.exists()) vfsFile.mkdir()
            val plugin: DrillPluginFile = vfsFile["agent-part.jar"]
            file.writeToFile(plugin)
            bl(pluginId, plugin)
        }

    }
    WsRouter.mapper[destination] = fileTopic
    return fileTopic
}

open class Topic(open val destination: String)

class GenericTopic<T>(
    override val destination: String,
    private val deserializer: DeserializationStrategy<T>,
    val block: suspend (T) -> Unit
) : Topic(destination) {
    suspend fun deserializeAndRun(message: String) {
        block(Json().parse(deserializer, message))
    }
}

class InfoTopic(override val destination: String, val block: suspend (String) -> Unit) : Topic(destination)

open class FileTopic(
    override val destination: String,
    open val block: (message: String, file: ByteArray) -> Unit
) : Topic(destination)

class PluginTopic(
    override val destination: String,
    newBlock: (message: String, plugin: ByteArray) -> Unit
) : FileTopic(destination, newBlock)
