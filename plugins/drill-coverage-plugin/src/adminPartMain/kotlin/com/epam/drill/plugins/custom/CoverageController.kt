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

@Suppress("unused")

class CoverageController(private val ws: WsService, val name: String) : AdminPluginPart(ws, name) {

    val initialClassBytes = mutableMapOf<String, ByteArray>()

    override suspend fun processData(dm: DrillMessage): Any {
        val sessionId = dm.sessionId
        val content = dm.content
        val parse = JSON.parse(CoverageMessage.serializer(), content!!)
        if (parse.type == CoverageEventType.COVERAGE_DATA) {
            val coverageBuilder = CoverageBuilder()
            val dataStore = ExecutionDataStore()
            val analyzer = Analyzer(dataStore, coverageBuilder)

            // Get new probes from message and populate dataStore with them
            val probes = JSON.parse(ExDataTemp.serializer().list, parse.data)
            probes.forEach { exData ->
                dataStore.put(ExecutionData(exData.id, exData.className, exData.probes.toBooleanArray()))
            }

            // Analyze all exiting classes
            dataStore.contents.forEach { exData ->
                analyzer.analyzeClass(initialClassBytes[exData.name], exData.name)
            }

            // TODO possible to store existing bundles to work with obsolete coverage results
            val a = coverageBuilder.getBundle("all")

            // TODO remove this temporary trace block and dependent methods
            val uncoveredMethodsCount = a.methodCounter.missedCount
            var coverage = a.methodCounter.coveredRatio * 100
            coverage = if (coverage.isFinite()) coverage else 0.0

            // TODO extend destination with plugin id
            ws.convertAndSend("/coverage", JSON.stringify(
                CoverageBlock.serializer(),
                CoverageBlock(coverage, uncoveredMethodsCount)
            ))

            println("General method coverage $coverage")
            println("General uncovered methods count $coverage")
            println("\n----------------\nCoverage of ${a.name}")
            printCounter("instructions", a.instructionCounter)
            printCounter("branches", a.branchCounter)
            printCounter("lines", a.lineCounter)
            printCounter("methods", a.methodCounter)
            printCounter("complexity", a.complexityCounter)
            for (p in a.packages) {
                println("\n----------------\nCoverage of package ${p.name}")
                printCounter("instructions", p.instructionCounter)
                printCounter("branches", p.branchCounter)
                printCounter("lines", p.lineCounter)
                printCounter("methods", p.methodCounter)
                printCounter("complexity", p.complexityCounter)
                for (c in p.classes) {
                    println("\n----------------\nCoverage of class ${c.name}")
                    printCounter("instructions", c.instructionCounter)
                    printCounter("branches", c.branchCounter)
                    printCounter("lines", c.lineCounter)
                    printCounter("methods", c.methodCounter)
                    printCounter("complexity", c.complexityCounter)
                    for (m in c.methods) {
                        println("\n----------------\nCoverage of method ${m.name}")
                        printCounter("instructions", m.instructionCounter)
                        printCounter("branches", m.branchCounter)
                        printCounter("lines", m.lineCounter)
                        printCounter("methods", m.methodCounter)
                        printCounter("complexity", m.complexityCounter)
                        println()
                        for (i in m.firstLine..m.lastLine) {
                            println("Line ${Integer.valueOf(i)}: ${getColor(m.getLine(i).status)}")
                        }
                    }
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

@Serializable
data class CoverageBlock(val coverage: Double, val uncoveredMethodsCount: Int)
