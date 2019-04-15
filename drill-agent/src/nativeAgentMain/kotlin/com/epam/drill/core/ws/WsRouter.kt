package com.epam.drill.core.ws

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import com.epam.drill.core.agentInfo
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.core.exec
import com.epam.drill.core.plugin.dumpConfigToFileSystem
import com.epam.drill.core.plugin.loader.loadPlugin
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.core.util.dumpConfigToFileSystem
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.PluginManager
import com.epam.drill.plugin.PluginStorage
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.UnloadReason
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.writeToFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.ThreadLocal

val topicLogger
    get() = DLogger("topicLogger")


@ExperimentalUnsignedTypes
fun topicRegister() =
    WsRouter {
        topic("/plugins/load").withPluginFileTopic { pluginId, plugin ->
            topicLogger.warn { "Load event. Plugin id is $pluginId" }
            if (PluginManager[pluginId] != null) {
                topicLogger.warn { "plugin '$pluginId' already is loaded" }
            } else {
                loadPlugin(plugin)
            }
        }

        topic("/plugins/unload").rawMessage { pluginId ->
            topicLogger.warn { "Unload event. Plugin id is $pluginId" }
            PluginManager[pluginId]?.unload(UnloadReason.ACTION_FROM_ADMIN)
            println(
                """
                |________________________________________________________
                |Physical Deletion is not implemented yet.
                |We should unload all resource e.g. classes, jars, .so/.dll
                |Try to create custom classLoader. After this full GC.
                |________________________________________________________
            """.trimMargin()
            )
        }

        topic("/plugins/agent-attached").rawMessage {
            topicLogger.warn { "Agent is attached" }
        }

        topic("/plugins/updatePluginConfig").withGenericTopic(PluginBean.serializer()) { config ->
            topicLogger.warn { "updatePluginConfig event: message is $config " }
            val agentPluginPart = PluginManager[config.id]
            if (agentPluginPart != null) {
                agentPluginPart.updateRawConfig(config)
                agentPluginPart.np?.updateRawConfig(config)
                config.dumpConfigToFileSystem()
                topicLogger.warn { "new settings for ${config.id} was save to file" }
            } else
                topicLogger.warn { "Plugin ${config.id} not loaded to agent" }

        }
        topic("/plugins/action").rawMessage { action ->
            topicLogger.warn { "actionPluign event: message is $action " }
            exec { loadedClasses }.forEach {
                println(it)
            }
            //fixme thi hardcode
            val agentPluginPart = exec { pInstrumentedStorage["coverage"] }
            agentPluginPart?.doRawAction(action)

        }

        topic("/plugins/togglePlugin").rawMessage { pluginId ->
            topicLogger.warn { "togglePlugin event: PluginId is $pluginId" }
            val agentPluginPart = PluginManager[pluginId]
            if (agentPluginPart != null)
                agentPluginPart.enabled = !agentPluginPart.enabled
            else
                topicLogger.warn { "Plugin $pluginId not loaded to agent" }

        }

        topic("/agent/updateAgentConfig").withGenericTopic(AgentInfo.serializer()) { info ->
            topicLogger.warn { "updateAgentConfig event: Info is $info" }
            agentInfo = info
        }

        topic("agent/toggleStandBy").rawMessage {
            topicLogger.warn { "toggleStandBy event" }
            toggleStandby(agentInfo)
            agentInfo.isEnable = !agentInfo.isEnable
            agentInfo.dumpConfigToFileSystem()
        }
    }

fun toggleStandby(agentInfo: AgentInfo) {
    val toggle: (AgentPart<*>) -> Unit =
        if (agentInfo.isEnable) { plugin -> PluginManager[plugin.id]?.off() } else { x -> PluginManager[x.id]?.on() }

    PluginStorage.storage.forEach {
        if (pluginConfigById(it.value.id).enabled)
            toggle(it.value)
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
        fun withFileTopic(block: (message: String, file: ByteArray) -> Unit): FileTopic {
            val fileTopic = FileTopic(destination, block)
            mapper[destination] = fileTopic
            return fileTopic
        }


        fun rawMessage(block: suspend (String) -> Unit): InfoTopic {
            val infoTopic = InfoTopic(destination, block)
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
