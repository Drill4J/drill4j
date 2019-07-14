package com.epam.drill.plugins.coverage

import com.epam.drill.*
import com.epam.drill.plugin.api.processing.*
import com.epam.drill.session.*
import kotlinx.atomicfu.*
import org.jacoco.core.internal.data.*

@Suppress("unused")
class CoverageAgentPart @JvmOverloads constructor(
    override val id: String,
    private val instrContext: SessionProbeArrayProvider = DrillProbeArrayProvider
) : AgentPart<CoverConfig, Action>(), InstrumentationPlugin {

    override val confSerializer = CoverConfig.serializer()

    override val serDe = commonSerDe

    val instrumenter = instrumenter(instrContext)

    private val _loadedClasses = atomic(emptyMap<String, Long?>())

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
            sendMessage(ClassBytes(className, bytes.encode()))
            val classId = CRC64.classId(bytes)
            className to classId

        }.toMap()
        _loadedClasses.value = loadedClasses
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
        return _loadedClasses.value[className]?.let { classId ->
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
        println("Plugin $id initialized")

    }


    override suspend fun doAction(action: Action) {
        when (action) {
            is StartSession -> {
                val sessionId = action.payload.sessionId
                val testType = action.payload.startPayload.testType
                println("Start recording for session $sessionId")
                instrContext.start(sessionId, testType)
                sendMessage(SessionStarted(ts = currentTimeMillis()))
            }
            is StopSession -> {
                val sessionId = action.payload.sessionId
                println("End of recording for session $sessionId")
                val runtimeData = instrContext.stop(sessionId) ?: emptySequence()
                if (runtimeData.any()) {
                    runtimeData.map { datum ->
                        ExDataTemp(
                            id = datum.id,
                            className = datum.name,
                            probes = datum.probes.toList(),
                            testType = datum.testType,
                            testName = datum.testName
                        )
                    }.chunked(10)
                        .forEach { dataChunk ->
                            //send data in chunks of 10
                            sendMessage(CoverDataPart(dataChunk))
                        }
                    sendMessage(SessionFinished(ts = currentTimeMillis()))
                }
            }
            is CancelSession -> {
                val sessionId = action.payload.sessionId
                println("Cancellation of recording for session $sessionId")
                instrContext.cancel(sessionId)
                sendMessage(SessionCancelled(ts = currentTimeMillis()))
            }
            else -> Unit
        }

    }

    private fun sendMessage(message: CoverMessage) {
        val messageStr = CoverMessage.serializer() stringify message
        Sender.sendMessage(id, messageStr)
    }
}