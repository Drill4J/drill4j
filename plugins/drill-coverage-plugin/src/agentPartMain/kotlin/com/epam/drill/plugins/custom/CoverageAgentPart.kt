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
        val runtime = sessionId?.let { sessionRuntimes[it] }
        return runtime?.run {
            getExecutionData(id, name, probeCount).probes
        } ?: BooleanArray(probeCount)
    }

    fun start(sessionId: String) {
        sessionRuntimes[sessionId] = RuntimeData()
    }

    fun stop(sessionId: String) = sessionRuntimes.remove(sessionId)
}

@Suppress("unused")
class CoveragePlugin(override val id: String) : InstrumentedPlugin<CoverConfig, CoverageAction>() {

    val initialClassBytes = mutableMapOf<String, ByteArray>()

    val instrumenter = instrumenter(DrillProbeArrayProvider)

    override fun doAction(action: CoverageAction) {
        val record = action.isRecord
        if (record) {
            println("Start recording for session ${action.sessionId}")
            DrillProbeArrayProvider.start(action.sessionId)
        } else if (!record) {
            println("End of recording for session ${action.sessionId}")
            val get = DrillProbeArrayProvider.stop(action.sessionId)
            if (get != null) {
                val dataStore = ExecutionDataStore()
                val sessionInfos = SessionInfoStore()
                get.collect(dataStore, sessionInfos, false)
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

    override fun initPlugin() {
    }

    override fun instrument(className: String, initialBytest: ByteArray): ByteArray {
        try {
            initialClassBytes[className] = initialBytest
            sendClass(ClassBytes(className, initialBytest.toList()))
            return instrumenter(className, initialBytest)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            throw ex
        }
    }

    private fun sendClass(classBytes: ClassBytes) {
        val classJson = JSON.stringify(ClassBytes.serializer(), classBytes)
        val message = JSON.stringify(
            CoverageMessage.serializer(),
            CoverageMessage(CoverageEventType.CLASS_BYTES, classJson)
        )
        Sender.sendMessage("coverage", message)
    }

    private fun sendExecutionData(exData: List<ExDataTemp>) {
        val exDataJson = JSON.stringify(ExDataTemp.serializer().list, exData)
        val message = JSON.stringify(
            CoverageMessage.serializer(),
            CoverageMessage(CoverageEventType.COVERAGE_DATA, exDataJson)
        )
        Sender.sendMessage("coverage", message)
    }


    override var confSerializer: kotlinx.serialization.KSerializer<CoverConfig> = CoverConfig.serializer()
    override var actionSerializer: kotlinx.serialization.KSerializer<CoverageAction> = CoverageAction.serializer()

}
