package com.epam.drill.core.ws

import com.epam.drill.JarVfsFile
import com.epam.drill.common.AgentInfo
import com.epam.drill.core.agentInfo
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.core.loadPlugin
import com.epam.drill.extractPluginFacilitiesTo
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.PluginStorage
import com.epam.drill.plugin.api.processing.Reason
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.file.writeToFile
import jvmapi.AddToSystemClassLoaderSearch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.ThreadLocal


fun topicRegister() =
    WsRouter {
        topic("/plugins/load").withPluginFileTopic { pluginId, plugin ->
            runBlocking {
                if (PluginManager[pluginId] != null) {
                    wsLogger.warn { "plugin '$pluginId' already is loaded" }
                } else
                    try {
                        plugin.extractPluginFacilitiesTo(localVfs(plugin.parent.absolutePath)) { vf ->
                            !vf.baseName.contains("nativePart") &&
                                    !vf.baseName.contains("static")
                        }
                        //init env...
                        com.epam.drill.jvmapi.currentEnvs()
                        AddToSystemClassLoaderSearch(plugin.absolutePath)
                        loadPlugin(plugin)
                        wsLogger.info { "load $pluginId" }
                    } catch (ex: Exception) {
                        wsLogger.error { "cant load the plugin..." }
                        ex.printStackTrace()
                    }
            }

        }

        topic("/plugins/unload").rawMessage { pluginId ->
            PluginManager[pluginId]?.unload(Reason.ACTION_FROM_ADMIN)
            //fixme physical deletion
        }

        topic("/plugins/on").rawMessage { pluginId ->
            PluginManager[pluginId]?.on()
        }

        topic("/plugins/off").rawMessage { pluginId ->
            PluginManager[pluginId]?.off()
        }

        topic("/plugins/agent-attached").rawMessage {
            println("Agent is attached")
        }

        topic("/plugins/updatePluginConfig").withGenericTopic(Config.serializer()) { config ->
            val agentPluginPart = PluginManager[config.pluginId]
            if (agentPluginPart != null) {
                agentPluginPart.updateRawConfig(config.content)
                agentPluginPart.np?.updateRawConfig(config.content)
            } else
                wsLogger.warn { "Plugin ${config.pluginId} not loaded to agent" }
        }


        topic("/plugins/togglePlugin").rawMessage { pluginId ->
            println("pluginId = $pluginId")
            val agentPluginPart = PluginManager[pluginId]
            if (agentPluginPart != null) {
                if (agentPluginPart.enabled) {
                    agentPluginPart.off()
                } else {
                    agentPluginPart.on()
                }
            }
            else
                wsLogger.warn { "Plugin $pluginId not loaded to agent" }
        }

        topic("/agent/updateAgentConfig").withGenericTopic(AgentInfo.serializer()) { info ->
            runBlocking {
                println(info)
                agentInfo = info
                val data = Json().stringify(
                    AgentInfo.serializer(),
                    agentInfo
                )
                resourcesVfs["$drillInstallationDir/configs/drillConfig.json"].writeString(data)
                println(data)
            }
        }

        topic("agent/toggleStandBy").rawMessage {
            if (agentInfo.isEnable) {
                agentInfo.rawPluginNames.forEach {
                    //todo save state to cofig file
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

    fun topic(url: String): inners {
        return inners(url)
    }

    @Suppress("ClassName")
    open class inners(open val destination: String) {
        fun <T> withGenericTopic(des: DeserializationStrategy<T>, block: (T) -> Unit): GenericTopic<T> {
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


        fun rawMessage(xx: (String) -> Unit): InfoTopic {
            val infoTopic = InfoTopic(destination, xx)
            mapper[destination] = infoTopic
            return infoTopic
        }


    }

    operator fun get(topic: String): Topic? {
        return mapper[topic]
    }

}


fun WsRouter.inners.withPluginFileTopic(bl: (message: String, plugin: JarVfsFile) -> Unit): FileTopic {
    val fileTopic = PluginTopic(destination) { pluginId, file ->
        runBlocking {
            val pluginsDir = localVfs(drillInstallationDir)["drill-plugins"]
            if (!pluginsDir.exists()) pluginsDir.mkdir()
            val vfsFile = pluginsDir[pluginId]
            if (!vfsFile.exists()) vfsFile.mkdir()
            val plugin: JarVfsFile = vfsFile["agent-part.jar"]
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
    val block: (T) -> Unit
) : Topic(destination) {
    fun deserializeAndRun(message: String) {
        block(Json().parse(deserializer, message))
    }
}

class InfoTopic(override val destination: String, val block: (String) -> Unit) : Topic(destination)

open class FileTopic(
    override val destination: String,
    open val block: (message: String, file: ByteArray) -> Unit
) : Topic(destination)

class PluginTopic(
    override val destination: String,
    newBlock: (message: String, plugin: ByteArray) -> Unit
) : FileTopic(destination, newBlock)

@Serializable
data class Config(val pluginId: String, val content: String)