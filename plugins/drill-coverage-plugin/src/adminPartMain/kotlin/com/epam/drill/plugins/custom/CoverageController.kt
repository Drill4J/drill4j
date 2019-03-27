package com.epam.drill.plugins.custom


import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import java.lang.Math.round

@Suppress("unused")

class CoverageController(private val ws: WsService, val name: String) : AdminPluginPart(ws, name) {

    val initialClassBytes = mutableMapOf<String, ByteArray>()

    override suspend fun processData(dm: DrillMessage): Any {
        val sessionId = dm.sessionId
        val content = dm.content
        val parse = JSON.parse(CoverageMessage.serializer(), content!!)
        if (parse.type == CoverageEventType.COVERAGE_DATA) {
            val xx = JSON.parse(ExDataTemp.serializer().list, parse.data)
            val executionData = ExecutionDataStore()
            xx.forEach { executionData.put(ExecutionData(it.id, it.className, it.probes.toBooleanArray())) }

            val coverageBuilder = CoverageBuilder()
            val analyzer = Analyzer(executionData, coverageBuilder)

            executionData.contents.forEach { x ->
                val name = x.name
                analyzer.analyzeClass(initialClassBytes[name], name)
            }
            val i1 =
                (coverageBuilder.getClasses().map { it.methodCounter.coveredCount }.sum() * 100.0) / coverageBuilder.getClasses().map { it.methodCounter.totalCount }.sum()
//            println(i1.round(2))

            for (cc in coverageBuilder.getClasses()) {
                        println("Coverage of class ${cc.getName()}")

                        printCounter("instructions", cc.instructionCounter)
                        printCounter("branches", cc.branchCounter)
                        printCounter("lines", cc.lineCounter)
                        printCounter("methods", cc.methodCounter)
                        printCounter("complexity", cc.complexityCounter)

                        for (i in cc.firstLine..cc.lastLine) {
                            System.out.printf(
                                "Line %s: %s%n", Integer.valueOf(i),
                                getColor(cc.getLine(i).status)
                            )
                        }
                    }
        } else if (parse.type == CoverageEventType.CLASS_BYTES) {
            val xx = JSON.parse(ClassBytes.serializer(), parse.data)
            initialClassBytes[xx.className] = xx.bytes.toByteArray()
        }
        return ""
    }

    private fun printCounter(unit: String, counter: ICounter) {
        val missed = Integer.valueOf(counter.missedCount)
        val total = Integer.valueOf(counter.totalCount)
        System.out.printf("%s of %s %s missed%n", missed, total, unit)
    }

    private fun getColor(status: Int): String {
        when (status) {
            ICounter.NOT_COVERED -> return "red"
            ICounter.PARTLY_COVERED -> return "yellow"
            ICounter.FULLY_COVERED -> return "green"
        }
        return ""
    }

}


//fixme these duplications. Move to common module
@Serializable
data class ExDataTemp(val id: Long, val className: String, val probes: List<Boolean>)


@Serializable
data class CoverageMessage(val type: CoverageEventType, val data: String)


enum class CoverageEventType {
    CLASS_BYTES, COVERAGE_DATA
}

@Serializable
data class ClassBytes(val className: String, val bytes: List<Byte>)


