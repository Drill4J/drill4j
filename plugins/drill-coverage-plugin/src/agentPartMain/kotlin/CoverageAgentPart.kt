package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.processing.InstrumentedPlugin
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.session.DrillRequest
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

    override fun initPlugin() {
        val str = "Plugin $id initialized!"
        val message = JSON.stringify(
            CoverageMessage.serializer(),
            CoverageMessage(CoverageEventType.INIT, str)
        )
        Sender.sendMessage("coverage", message)
        println(str)
    }

    override fun instrument(className: String, initialBytes: ByteArray): ByteArray {
        return if ("_\$\$_" !in className && "CGLIB\$\$" !in className) {
            try {
                sendClass(ClassBytes(className, initialBytes.toList()))
                instrumenter(className, initialBytes)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                throw ex
            }
        } else initialBytes
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
