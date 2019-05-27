package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.InstrumentationPlugin
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.plugin.api.processing.UnloadReason
import com.epam.drill.session.DrillRequest
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list

val instrContext = object : InstrContext {
    override fun invoke(): String? = DrillRequest.currentSession()

    override fun get(key: String): String? = DrillRequest[key.toLowerCase()]
}

private val json = Json.Companion


object DrillProbeArrayProvider : SimpleSessionProbeArrayProvider(instrContext)

@UnstableDefault
@Suppress("unused")
class CoveragePlugin @JvmOverloads constructor(
    override val id: String,
    private val instrContext: SessionProbeArrayProvider = DrillProbeArrayProvider
) : AgentPart<CoverConfig, Action>(), InstrumentationPlugin {
    override fun doRawAction(action: String) {
        doAction(Json.parse(actionSerializer, action))
    }

    override fun on() {

    }

    override fun off() {

    }

    override fun destroyPlugin(unloadReason: UnloadReason) {

    }

    override fun retransform() {
        val map = classNameSet.map {
            try {
                ClassLoader.getSystemClassLoader().loadClass(it.replace("/", "."))
            } catch (ex: Throwable) {
//                println("))")
                null
            }
        }
        DrillRequest.RetransformClasses(map.filterNotNull().toTypedArray())
    }

    val instrumenter = instrumenter(instrContext)

    private val classNameSet = mutableSetOf<String>()

    override fun initPlugin() {
        val initializingMessage = "Initializing plugin $id...\nConfig: ${config.message}"
        val classPath1 = ClassPath()
        val scanItPlease = classPath1.scanItPlease(ClassLoader.getSystemClassLoader())
        val filter = scanItPlease.filter { (k, v) ->
            config.pathPrefixes.any {
                k.removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
                    .removeSuffix(".class").startsWith(it)
            }
        }


        val initInfo = InitInfo(filter.count(), initializingMessage)
        sendMessage(CoverageEventType.INIT, json.stringify(InitInfo.serializer(), initInfo))
        filter.forEach { (resourceName, classInfo) ->
            classNameSet.add(resourceName.removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
                .removeSuffix(".class"))
            val bytes = classInfo.url(resourceName).readBytes()
            sendClass(ClassBytes(resourceName.removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
                .removeSuffix(".class"), bytes.toList()))
        }
        val initializedStr = "Plugin $id initialized!"
        sendMessage(CoverageEventType.INITIALIZED, initializedStr)
        println(initializedStr)
        println("Loaded ${classNameSet.count()} classes: $classNameSet")
    }


    override fun doAction(action: Action) {
        val sessionId = action.payload.sessionId
        when (action.type) {
            ActionType.START -> {
                println("Start recording for session $sessionId")
                instrContext.start(sessionId)
                sendMessage(CoverageEventType.SESSION_STARTED, sessionId)
            }
            ActionType.STOP -> {
                println("End of recording for session $sessionId")
                val runtimeData = instrContext.stop(sessionId)
                runtimeData?.apply {
                    val dataToSend = map { datum ->
                        ExDataTemp(
                            id = datum.id,
                            className = datum.name,
                            probes = datum.probes.toList(),
                            testName = datum.testName
                        )
                    }
                    //send data in chunk of 10
                    dataToSend.chunked(10) { dataChunk ->
                        sendExecutionData(dataChunk)
                    }
                    sendMessage(CoverageEventType.SESSION_FINISHED, sessionId)
                }
            }
            ActionType.CANCEL -> {
                println("Cancellation of recording for session $sessionId")
                instrContext.cancel(sessionId)
                sendMessage(CoverageEventType.SESSION_CANCELLED, sessionId)
            }
        }

    }

    override fun instrument(className: String, initialBytes: ByteArray): ByteArray? {
        return if (className in classNameSet) { //instrument only classes from the selected packages
            try {
                instrumenter(className, initialBytes)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                throw ex
            }
        } else null
    }

    private fun sendClass(classBytes: ClassBytes) {
        val classJson = json.stringify(ClassBytes.serializer(), classBytes)
        sendMessage(CoverageEventType.CLASS_BYTES, classJson)
    }

    private fun sendExecutionData(exData: List<ExDataTemp>) {
        val exDataJson = json.stringify(ExDataTemp.serializer().list, exData)
        sendMessage(CoverageEventType.COVERAGE_DATA_PART, exDataJson)
    }

    private fun sendMessage(type: CoverageEventType, str: String) {
        val message = json.stringify(
            CoverageMessage.serializer(),
            CoverageMessage(type, str)
        )
        Sender.sendMessage("coverage", message)
    }


    override var confSerializer: kotlinx.serialization.KSerializer<CoverConfig> = CoverConfig.serializer()
    override var actionSerializer: kotlinx.serialization.KSerializer<Action> = Action.serializer()

}
