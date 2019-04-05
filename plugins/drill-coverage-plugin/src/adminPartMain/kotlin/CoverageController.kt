package com.epam.drill.plugins.coverage


import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.analysis.*
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.NewObject

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

                // Analyze all existing classes
                initialClassBytes.forEach { (name, bytes) ->
                    analyzer.analyzeClass(bytes, name)
                }

                // TODO possible to store existing bundles to work with obsolete coverage results
                val bundleCoverage = coverageBuilder.getBundle("all")

                val totalLinePercent = bundleCoverage.methodCounter.coveredRatio * 100
                val totalCoverage = if (totalLinePercent.isFinite()) totalLinePercent else null

                fillJavaClasses(bundleCoverage)

                val classesCount = bundleCoverage.classCounter.totalCount
                val methodsCount = bundleCoverage.methodCounter.totalCount
                val uncoveredMethodsCount = bundleCoverage.methodCounter.missedCount

                val coverageBlock = CoverageBlock(
                    coverage = totalCoverage,
                    classesCount = classesCount,
                    methodsCount = methodsCount,
                    uncoveredMethodsCount = uncoveredMethodsCount
                )
                println(coverageBlock)
                ws.convertAndSend("/coverage", JSON.stringify(CoverageBlock.serializer(), coverageBlock))

                //TODO Diff should be calculated after all classes has been parsed
                val diff = javers.compareCollections(
                    prevJavaClasses.values.toList(),
                    javaClasses.values.toList(),
                    JavaClass::class.java
                )
                val newMethods = diff.getObjectsByChangeType(NewObject::class.java).filterIsInstance<JavaMethod>()
                val newCoverageBlock = if (newMethods.isNotEmpty()) {
                    println("New methods count: ${newMethods.count()}")
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
                    NewCoverageBlock(
                        newMethodsCoverages.count(),
                        newMethodsCoverages.count { it.methodCounter.coveredCount > 0 },
                        newCoverage * 100
                    )
                } else NewCoverageBlock()
                println(newCoverageBlock)

                // TODO extend destination with plugin id
                ws.convertAndSend(
                    "/coverage-new",
                    JSON.stringify(NewCoverageBlock.serializer(), newCoverageBlock)
                )

                val classesCoverage = classesCoverage(bundleCoverage)
                println(classesCoverage)
                ws.convertAndSend(
                    "/coverage-by-classes",
                    JSON.stringify(JavaClassCoverage.serializer().list, classesCoverage)
                )
            }
            CoverageEventType.CLASS_BYTES -> {
                val classData = JSON.parse(ClassBytes.serializer(), parse.data)
                val className = classData.className
                val bytes = classData.bytes.toByteArray()
                initialClassBytes[className] = bytes
            }
        }
        return ""
    }

    private fun fillJavaClasses(bundleCoverage: IBundleCoverage) {
        javaClasses.clear()
        bundleCoverage.packages
            .flatMap { it.classes }
            .map { cc ->
                cc.name to JavaClass(
                    name = cc.name.substringAfterLast('/'),
                    path = cc.name,
                    methods = cc.methods.map {
                        JavaMethod(
                            ownerClass = cc.name,
                            name = it.name,
                            desc = it.desc
                        )
                    }.toSet()

                )
            }.toMap(javaClasses)
    }

    private fun classesCoverage(bundleCoverage: IBundleCoverage): List<JavaClassCoverage> = bundleCoverage.packages
        .flatMap { it.classes }
        .map { classCoverage ->
            JavaClassCoverage(
                name = classCoverage.name.substringAfterLast('/'),
                path = classCoverage.name,
                coverage = classCoverage.coverage(),
                totalMethodsCount = classCoverage.methodCounter.totalCount,
                coveredMethodsCount = classCoverage.methodCounter.coveredCount,
                methods = classCoverage.methods.map { methodCoverage ->
                    if (!methodCoverage.lineCounter.coveredRatio.isFinite()) {
                        println(">>>>>>${classCoverage.name}/${methodCoverage.name}${methodCoverage.desc}")
                    }
                    JavaMethodCoverage(
                        name = methodCoverage.name,
                        desc = methodCoverage.desc,
                        coverage = methodCoverage.coverage()
                    )
                }.toList()
            )
        }.toList()
}

fun IClassCoverage.coverage() : Double {
    val ratio = this.lineCounter.coveredRatio
    return if (ratio.isFinite()) ratio * 100.0 else 100.0
}

fun IMethodCoverage.coverage() : Double {
    val ratio = this.lineCounter.coveredRatio
    return if (ratio.isFinite()) ratio * 100.0 else 100.0
}

