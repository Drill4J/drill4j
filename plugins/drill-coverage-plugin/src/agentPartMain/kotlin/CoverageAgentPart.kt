package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.processing.InstrumentedPlugin
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.session.DrillRequest
import com.google.common.reflect.ClassPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.list

val instrContext = object : InstrContext {
    override fun invoke(): String? = DrillRequest.currentSession()

    override fun get(key: String): String? = DrillRequest[key.toLowerCase()]
}

private val json = Json {
    prettyPrint = true
}


object DrillProbeArrayProvider : SimpleSessionProbeArrayProvider(instrContext)

@Suppress("unused")
class CoveragePlugin @JvmOverloads constructor(
    override val id: String,
    private val instrContext: SessionProbeArrayProvider = DrillProbeArrayProvider
) : InstrumentedPlugin<CoverConfig, Action>() {

    val instrumenter = instrumenter(instrContext)

    private val classNameSet = mutableSetOf<String>()

    override fun initPlugin() {
        val initializingMessage = "Initializing plugin $id...\nConfig: ${config.message}"
        println(initializingMessage)
        @Suppress("UnstableApiUsage")
        val classPath = ClassPath.from(ClassLoader.getSystemClassLoader())
        val classInfoPairs = classPath.topLevelClasses.mapNotNull { classInfo ->
            val resourceName = classInfo.resourceName
                .removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
                .removeSuffix(".class")
            if (config.pathPrefixes.any { resourceName.startsWith(it) }) {
                Pair(resourceName, classInfo)
            } else null
        }.distinctBy { it.first }
        val initInfo = InitInfo(classInfoPairs.count(), initializingMessage)
        sendMessage(CoverageEventType.INIT, json.stringify(InitInfo.serializer(), initInfo))
        classInfoPairs.forEach { (resourceName, classInfo) ->
            classNameSet.add(resourceName)
            val bytes = classInfo.asByteSource().read()
            sendClass(ClassBytes(resourceName, bytes.toList()))
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
