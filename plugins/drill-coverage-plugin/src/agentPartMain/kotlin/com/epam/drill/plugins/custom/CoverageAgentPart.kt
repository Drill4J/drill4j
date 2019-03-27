package com.epam.drill.plugins.custom

import com.epam.drill.plugin.api.processing.InstrumentedPlugin
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.session.DrillRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.Es
import org.jacoco.core.runtime.MyRuntime
import java.util.concurrent.Callable


@Suppress("unused")
class CoveragePlugin(override val id: String) : InstrumentedPlugin<CoverConfig, CoverageAction>() {

    val initialClassBytes = mutableMapOf<String, ByteArray>()

    override fun doAction(action: CoverageAction) {
        val record = action.isRecord
        if (record) {
            println("Start record for session ${action.sessionId}")
            Es.get().workers.add(action.sessionId)
        } else if (!record) {
            println("End record for session ${action.sessionId}")
            Es.get().workers.remove(action.sessionId)
            val get = Es.get().sessionToRuntimeMap.remove(action.sessionId)
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
        Es.get().sessionCallable = object : Callable<String> {
            override fun call(): String {
                return DrillRequest.currentSession()
            }
        }
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

            return Instrumenter(MyRuntime()).instrument(initialBytest, className)
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


