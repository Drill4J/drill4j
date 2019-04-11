package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.processing.InstrumentedPlugin
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.session.DrillRequest
import com.google.common.reflect.ClassPath
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore

object DrillProbeArrayProvider : SimpleSessionProbeArrayProvider(DrillRequest::currentSession)

@Suppress("unused")
class CoveragePlugin @JvmOverloads constructor(
    override val id: String,
    private val probeArrayProvider: SessionProbeArrayProvider = DrillProbeArrayProvider
) : InstrumentedPlugin<CoverConfig, CoverageAction>() {

    val instrumenter = instrumenter(probeArrayProvider)
    
    private val classNameSet = mutableSetOf<String>()

    override fun initPlugin() {
        val initializingMessage = "Initializing plugin $id...\nConfig: ${config.message}"
        println(initializingMessage)
        sendMessage(CoverageEventType.INIT, initializingMessage)

        @Suppress("UnstableApiUsage")
        ClassPath.from(ClassLoader.getSystemClassLoader()).topLevelClasses.mapNotNull { classInfo ->
            val resourceName = classInfo.resourceName
                .removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
                .removeSuffix(".class")
            if (config.pathPrefixes.any { resourceName.startsWith(it) }) {
                Pair(resourceName, classInfo)
            } else null
        }.forEach { (resourceName, classInfo) ->
            classNameSet.add(resourceName)
            val bytes = classInfo.asByteSource().read()
            sendClass(ClassBytes(resourceName, bytes.toList()))
        }
        val initializedStr = "Plugin $id initialized!"
        sendMessage(CoverageEventType.INITIALIZED, initializedStr)
        println(initializedStr)
        println("Loaded ${classNameSet.count()} classes: $classNameSet")
    }

    override fun doAction(action: CoverageAction) {
        val record = action.isRecord
        if (record) {
            println("Start recording for session ${action.sessionId}")
            probeArrayProvider.start(action.sessionId)
        } else {
            println("End of recording for session ${action.sessionId}")
            val runtimeData = probeArrayProvider.stop(action.sessionId)
            runtimeData?.apply {
                val dataStore = ExecutionDataStore()
                val sessionInfos = SessionInfoStore()
                runtimeData.collect(dataStore, sessionInfos, false)
                sendExecutionData(dataStore.contents.map { exData ->
                    ExDataTemp(
                        exData.id,
                        exData.name,
                        exData.probes.toList()
                    )
                })
            }
        }
    }

    override fun instrument(className: String, initialBytes: ByteArray): ByteArray {
        return if (className in classNameSet) { //instrument only classes from the selected packages
            try {
                instrumenter(className, initialBytes)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                throw ex
            }
        } else initialBytes
    }

    private fun sendClass(classBytes: ClassBytes) {
        val classJson = JSON.stringify(ClassBytes.serializer(), classBytes)
        sendMessage(CoverageEventType.CLASS_BYTES, classJson)
    }

    private fun sendExecutionData(exData: List<ExDataTemp>) {
        val exDataJson = JSON.stringify(ExDataTemp.serializer().list, exData)
        sendMessage(CoverageEventType.COVERAGE_DATA, exDataJson)
    }

    private fun sendMessage(type: CoverageEventType, str: String) {
        val message = JSON.stringify(
            CoverageMessage.serializer(),
            CoverageMessage(type, str)
        )
        Sender.sendMessage("coverage", message)
    }


    override var confSerializer: kotlinx.serialization.KSerializer<CoverConfig> = CoverConfig.serializer()
    override var actionSerializer: kotlinx.serialization.KSerializer<CoverageAction> = CoverageAction.serializer()

}
