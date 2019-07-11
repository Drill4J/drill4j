package com.epam.drill.plugins.coverage

import com.epam.drill.*
import com.epam.drill.plugin.api.processing.*
import com.epam.drill.session.*
import org.jacoco.core.internal.data.*
import java.util.concurrent.atomic.*

@Suppress("unused")
class CoverageAgentPart @JvmOverloads constructor(
    override val id: String,
    private val instrContext: SessionProbeArrayProvider = DrillProbeArrayProvider
) : AgentPart<CoverConfig, Action>(), InstrumentationPlugin {

    override val confSerializer = CoverConfig.serializer()

    override val serDe = commonSerDe

    val instrumenter = instrumenter(instrContext)

    private val loadedClassesRef = AtomicReference<Map<String, Long?>>(emptyMap())

    override fun on() {
        val initializingMessage = "Initializing plugin $id...\nConfig: ${config.message}"
        val scanItPlease = ClassPath().scanItPlease(ClassLoader.getSystemClassLoader())
        val filter = scanItPlease
            .filter { (classPath, _) ->
                isTopLevelClass(classPath) && config.pathPrefixes.any { packageName ->
                    isAllowedClass(classPath, packageName)
                }
            }

        val initInfo = InitInfo(filter.count(), initializingMessage)
        sendMessage(initInfo)
        val loadedClasses = filter.map { (resourceName, classInfo) ->
            val className = resourceName
                .removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
                .removeSuffix(".class")
            val bytes = classInfo.url(resourceName).readBytes()

            sendMessage(ClassBytes(className, bytes.toList()))
            val classId = CRC64.classId(bytes)
            className to classId

        }.toMap()
        loadedClassesRef.set(loadedClasses)
        val initializedStr = "Plugin $id initialized!"
        sendMessage(Initialized(msg = initializedStr))
        println(initializedStr)
        println("Loaded ${loadedClasses.count()} classes")
        retransform()

    }

    override fun off() {
        retransform()
    }

    override fun instrument(className: String, initialBytes: ByteArray): ByteArray? {
        if (!enabled) return null
        return loadedClassesRef.get()[className]?.let { classId ->
            instrumenter(className, classId, initialBytes)
        }
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {

    }

    override fun retransform() {
        val filter = DrillRequest.GetAllLoadedClasses()
            .filter { cl -> cl.`package` != null && isTopLevelClass(cl.name) } // only top level classes
            .filter { cla ->
                val bytecodePackageView = cla.`package`.name.replace(".", "/")
                config.pathPrefixes.any { packageName ->
                    isAllowedClass(bytecodePackageView, packageName)
                }
            }
        DrillRequest.RetransformClasses(filter.toTypedArray())

        println("${filter.size} classes were re-transformed")
    }

    override fun initPlugin() {

    }


    override suspend fun doAction(action: Action) {
        when (action) {
            is StartSession -> {
                val sessionId = action.payload.sessionId
                val testType = action.payload.startPayload.testType
                println("Start recording for session $sessionId")
                instrContext.start(sessionId, testType)
                sendMessage(SessionStarted(ts = System.currentTimeMillis()))
            }
            is StopSession -> {
                val sessionId = action.payload.sessionId
                println("End of recording for session $sessionId")
                val runtimeData = instrContext.stop(sessionId)
                runtimeData?.apply {
                    val dataToSend = map { datum ->
                        ExDataTemp(
                            id = datum.id,
                            className = datum.name,
                            probes = datum.probes.toList(),
                            testType = datum.testType,
                            testName = datum.testName
                        )
                    }
                    //send data in chunk of 10
                    dataToSend.chunked(10) { dataChunk ->
                        sendMessage(CoverDataPart(dataChunk))
                    }
                    sendMessage(SessionFinished(ts = System.currentTimeMillis()))
                }
            }
            is CancelSession -> {
                val sessionId = action.payload.sessionId
                println("Cancellation of recording for session $sessionId")
                instrContext.cancel(sessionId)
                sendMessage(SessionCancelled(ts = System.currentTimeMillis()))
            }
            else -> Unit
        }

    }

    private fun sendMessage(message: CoverMessage) {
        val messageStr = CoverMessage.serializer() stringify message
        Sender.sendMessage(id, messageStr)
    }
}