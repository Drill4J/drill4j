package com.epam.drill.plugins.coverage


import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.NewObject
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

@Suppress("unused")
class CoverageController(private val ws: WsService, val name: String) : AdminPluginPart(ws, name) {

    val initialClassBytes = mutableMapOf<String, ByteArray>()
    
    val javaClasses = mutableMapOf<String, JavaClass>()

    //TODO Only the last prev state at this moment - use JaVers repositories
    val prevJavaClasses = mutableMapOf<String, JavaClass>()

    private val javers = JaversBuilder.javers().build()

    override suspend fun processData(dm: DrillMessage): Any {
        val sessionId = dm.sessionId
        val content = dm.content
        val parse = JSON.parse(CoverageMessage.serializer(), content!!)
        when(parse.type) {
            CoverageEventType.INIT -> {
                println(parse.data) //log init message
                //change maps
                initialClassBytes.clear()
                prevJavaClasses.clear()
                prevJavaClasses.putAll(javaClasses)
                javaClasses.clear()
            }
            CoverageEventType.COVERAGE_DATA -> {
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
                val bundleCoverage = coverageBuilder.getBundle("all")

                val uncoveredMethodsCount = bundleCoverage.methodCounter.missedCount
                var coveredPercent = bundleCoverage.methodCounter.coveredRatio * 100
                coveredPercent = if (coveredPercent.isFinite()) coveredPercent else 0.0

                println("General method coverage $coveredPercent")
                println("General uncovered methods count $uncoveredMethodsCount")
                
                println("Current java classes: $javaClasses")


                val newCoverageBlock = if (prevJavaClasses.isNotEmpty()) {
                    //TODO Diff should be calculated after all classes has been parsed
                    val diff = javers.compareCollections(
                        prevJavaClasses.values.toList(),
                        javaClasses.values.toList(),
                        JavaClass::class.java
                    )
                    val newMethods = diff.getObjectsByChangeType(NewObject::class.java).filterIsInstance<JavaMethod>()
                    if (newMethods.isNotEmpty()) {
                        println("New methods: $newMethods")
                        val newMethodSet = newMethods.toSet()
                        val newMethodsCoverages = bundleCoverage.packages
                            .flatMap { it.classes }
                            .flatMap { c -> c.methods.map { Pair(JavaMethod(c.name, it.name, it.desc), it) } }
                            .filter { it.first in newMethodSet }
                            .map { it.second }
                        val totalLineCount = newMethodsCoverages.sumBy { it.lineCounter.totalCount }
                        val coveredLineCount = newMethodsCoverages.sumBy { it.lineCounter.coveredCount }
                        //line coverage
                        val newCoverage = if (totalLineCount > 0) coveredLineCount.toDouble() / totalLineCount else 0.0
                        val newCoverageBlock = NewCoverageBlock(
                            newMethodsCoverages.count(),
                            newMethodsCoverages.count { it.methodCounter.coveredCount > 0 },
                            newCoverage * 100
                        )
                        println(newCoverageBlock)
                        newCoverageBlock
                    } else NewCoverageBlock()
                    
                } else NewCoverageBlock()
                ws.convertAndSend(
                    "/coverage-new",
                    JSON.stringify(NewCoverageBlock.serializer(), newCoverageBlock)
                )

                // TODO extend destination with plugin id
                ws.convertAndSend("/coverage", JSON.stringify(
                    CoverageBlock.serializer(),
                    CoverageBlock(coveredPercent, uncoveredMethodsCount)
                ))
                debugPrintCoverage(bundleCoverage)
            }
            CoverageEventType.CLASS_BYTES -> {
                val classData = JSON.parse(ClassBytes.serializer(), parse.data)
                val className = classData.className
                val bytes = classData.bytes.toByteArray()
                //ignore instrumented classes/methods (CGLIB, Entities, etc)
                if ("_\$\$_" !in className && "CGLIB\$\$" !in className) {
                    javaClasses[className] = parse(bytes)
                }
                initialClassBytes[className] = bytes
            }
        }
        return ""
    }

    private fun parse(data: ByteArray): JavaClass {
        val classNode = ClassNode()
        val classReader = ClassReader(data)
        classReader.accept(classNode, ClassReader.SKIP_DEBUG)
        val path = classNode.name
        val methods = classNode.methods.map { JavaMethod(path, it.name, it.desc) }.toSet()
        val name = path.substringAfterLast('/')
        return JavaClass(name, path, methods)
    }


    private fun debugPrintCoverage(bundleCoverage: IBundleCoverage) {
        println("\n----------------\nCoverage of ${bundleCoverage.name}")
        printCounter("instructions", bundleCoverage.instructionCounter)
        printCounter("branches", bundleCoverage.branchCounter)
        printCounter("lines", bundleCoverage.lineCounter)
        printCounter("methods", bundleCoverage.methodCounter)
        printCounter("complexity", bundleCoverage.complexityCounter)
        for (p in bundleCoverage.packages) {
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
                    println("\n----------------\nCoverage of method ${m.name} ${m.desc}")
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
    }

    private fun printCounter(unit: String, counter: ICounter) {
        val missed = Integer.valueOf(counter.missedCount)
        val total = Integer.valueOf(counter.totalCount)
        println("$missed of $total $unit missed")
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

