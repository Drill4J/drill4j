package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.InstrumentedPlugin
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.session.DrillRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore
import org.jacoco.core.runtime.RuntimeData
import java.util.concurrent.ConcurrentHashMap

object DrillProbeArrayProvider : ProbeArrayProvider {
    
    private val sessionRuntimes = ConcurrentHashMap<String, RuntimeData>() 
        
    override fun invoke(id: Long, name: String, probeCount: Int): BooleanArray {
        val sessionId = DrillRequest.currentSession()
        return sessionRuntimes[sessionId]?.run {
            getExecutionData(id, name, probeCount).probes
        } ?: BooleanArray(probeCount)
    }
    
    fun start(sessionId: String) {
        sessionRuntimes.put(sessionId, RuntimeData())
    }

    fun stop(sessionId: String) = sessionRuntimes.remove(sessionId)
}

val instrumenter = instrumenter(DrillProbeArrayProvider)

@Suppress("unused")
class CoveragePlugin(override val id: String) : InstrumentedPlugin<CoverConfig, CoverageAction>() {

    val initialClassBytes = mutableMapOf<String, ByteArray>()

    override fun doAction(action: CoverageAction) {
        val record = action.isRecord
        if (record) {
            println("Start recording for session ${action.sessionId}")
            DrillProbeArrayProvider.start(action.sessionId)
        } else if (!record) {
            println("End recording for session ${action.sessionId}")
            val get = DrillProbeArrayProvider.stop(action.sessionId)
            if (get != null) {
                val executionData = ExecutionDataStore()
                val sessionInfos = SessionInfoStore()
                get.collect(executionData, sessionInfos, false)
                val map = executionData.contents.map { ExDataTemp(it.id, it.name, it.probes.toList()) }


                val message = JSON.stringify(ExDataTemp.serializer().list, map)
                val stringify = JSON.stringify(
                    CoverageMessage.serializer(),
                    CoverageMessage(CoverageEventType.COVERAGE_DATA, message)
                )
                Sender.sendMessage("coverage", stringify)


            }
        }
    }

    override fun initPlugin() {
    }

    override fun instrument(className: String, initialBytest: ByteArray): ByteArray {
        try {
            initialClassBytes[className] = initialBytest
            val classBytes = ClassBytes(className, initialBytest.toList())

            val message = JSON.stringify(
                CoverageMessage.serializer(),
                CoverageMessage(
                    CoverageEventType.CLASS_BYTES,
                    JSON.stringify(ClassBytes.serializer(), classBytes)
                )
            )
            Sender.sendMessage(
                "coverage",
                message
            )

            return instrumenter(className, initialBytest)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            throw ex
        }
    }


    override var confSerializer: kotlinx.serialization.KSerializer<CoverConfig> = CoverConfig.serializer()
    override var actionSerializer: kotlinx.serialization.KSerializer<CoverageAction> = CoverageAction.serializer()

}

@Serializable
data class CoverConfig(val message: String)


@Serializable
data class CoverageAction(
    val sessionId: String,
    val isRecord: Boolean
)


@Serializable
data class CoverageMessage(val type: CoverageEventType, val data: String)



enum class CoverageEventType {
    CLASS_BYTES, COVERAGE_DATA
}

@Serializable
data class ExDataTemp(val id: Long, val className: String, val probes: List<Boolean>)


@Serializable
data class ClassBytes(val className: String, val bytes: List<Byte>)


