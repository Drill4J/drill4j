package com.epam.drill.plugins.coverage


import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import org.jacoco.core.analysis.*
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import org.javers.core.diff.changetype.NewObject
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class CoverageController(private val ws: WsService, val name: String) : AdminPluginPart(ws, name) {

    val agentStates = ConcurrentHashMap<AgentInfo, AgentState>()

    override suspend fun processData(agentInfo: AgentInfo, dm: DrillMessage): Any {
        val agentState = agentStates.getOrPut(agentInfo) { AgentState(agentInfo) }
        val content = dm.content
        val message = JSON.parse(CoverageMessage.serializer(), content!!)
        return processData(agentState, message)
    }
    
    private suspend fun processData(agentState: AgentState, parse: CoverageMessage): Any {
        val agentInfo = agentState.agentInfo
        when (parse.type) {
            CoverageEventType.INIT -> {
                val initInfo = JSON.parse(InitInfo.serializer(), parse.data)
                agentState.init(initInfo)
                println(initInfo.message) //log init message
                println("${initInfo.classesCount} classes to load")
            }
            CoverageEventType.CLASS_BYTES -> {
                val classData = JSON.parse(ClassBytes.serializer(), parse.data)
                val className = classData.className
                val bytes = classData.bytes.toByteArray()
                agentState.addClass(className, bytes)
            }
            CoverageEventType.INITIALIZED -> {
                println(parse.data) //log initialized message
                agentState.initialized()
            }
            CoverageEventType.COVERAGE_DATA -> {
                // Analyze all existing classes
                val classesData = agentState.classesData()
                val initialClassBytes = classesData.classesBytes
                val javaClasses = classesData.javaClasses
                val prevJavaClasses = classesData.prevJavaClasses

                val coverageBuilder = CoverageBuilder()
                val dataStore = ExecutionDataStore()
                val analyzer = Analyzer(dataStore, coverageBuilder)

                // Get new probes from message and populate dataStore with them
                //also fill up assoc tests
                val probes = JSON.parse(ExDataTemp.serializer().list, parse.data)
                val assocTestsMap = probes.flatMap { exData ->
                    val probeArray = exData.probes.toBooleanArray()
                    val executionData = ExecutionData(exData.id, exData.className, probeArray.copyOf())
                    dataStore.put(executionData)
                    when (exData.testName) {
                        null -> emptyList()
                        else -> collectAssocTestPairs(
                            initialClassBytes,
                            ExecutionData(exData.id, exData.className, probeArray.copyOf()),
                            exData.testName
                        )
                    }
                }.groupBy({ it.first }) { it.second } //group by test names
                    .mapValues { (_, tests) -> tests.distinct() }
                val assocTests = assocTestsMap.map { (key, tests) ->
                    AssociatedTests(
                        id = key.id,
                        packageName = key.packageName,
                        className = key.className,
                        methodName = key.methodName,
                        tests = tests
                    )
                }
                if (assocTests.isNotEmpty()) {
                    println("Assoc tests - ids count: ${assocTests.count()}")
                    println(assocTests)
                    ws.convertAndSend(
                        agentInfo,
                        "/associated-tests",
                        JSON.stringify(AssociatedTests.serializer().list, assocTests)
                    )
                }


                initialClassBytes.forEach { (name, bytes) ->
                    analyzer.analyzeClass(bytes, name)
                }

                // TODO possible to store existing bundles to work with obsolete coverage results
                val bundleCoverage = coverageBuilder.getBundle("all")

                val totalCoveragePercent = bundleCoverage.coverage


                val classesCount = bundleCoverage.classCounter.totalCount
                val methodsCount = bundleCoverage.methodCounter.totalCount
                val uncoveredMethodsCount = bundleCoverage.methodCounter.missedCount

                val coverageBlock = CoverageBlock(
                    coverage = totalCoveragePercent,
                    classesCount = classesCount,
                    methodsCount = methodsCount,
                    uncoveredMethodsCount = uncoveredMethodsCount
                )
                println(coverageBlock)
                ws.convertAndSend(
                    agentInfo,
                    "/coverage",
                    JSON.stringify(CoverageBlock.serializer(), coverageBlock)
                )

                //TODO Diff should be calculated after all classes has been parsed
                val diff = agentState.javers.compareCollections(
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
                    val totalCount = newMethodsCoverages.sumBy { it.instructionCounter.totalCount }
                    val coveredCount = newMethodsCoverages.sumBy { it.instructionCounter.coveredCount }
                    //line coverage
                    val newCoverage = if (totalCount > 0) coveredCount.toDouble() / totalCount else 0.0
                    NewCoverageBlock(
                        newMethodsCoverages.count(),
                        newMethodsCoverages.count { it.methodCounter.coveredCount > 0 },
                        newCoverage * 100
                    )
                } else NewCoverageBlock()
                println(newCoverageBlock)

                // TODO extend destination with plugin id
                ws.convertAndSend(
                    agentInfo,
                    "/coverage-new",
                    JSON.stringify(NewCoverageBlock.serializer(), newCoverageBlock)
                )

                val classCoverage = classCoverage(bundleCoverage, assocTestsMap)
                println(classCoverage)
                ws.convertAndSend(
                    agentInfo,
                    "/coverage-by-classes",
                    JSON.stringify(JavaClassCoverage.serializer().list, classCoverage)
                )

                val packageCoverage = packageCoverage(bundleCoverage, assocTestsMap)
                println(packageCoverage)
                ws.convertAndSend(
                    agentInfo,
                    "/coverage-by-packages",
                    JSON.stringify(JavaPackageCoverage.serializer().list, packageCoverage)
                )
            }
        }
        return ""
    }

    private fun collectAssocTestPairs(
        initialClassBytes: Map<String, ByteArray>,
        executionData: ExecutionData,
        testName: String
    ): List<Pair<CoverageKey, String>> {
        val cb = CoverageBuilder()
        Analyzer(ExecutionDataStore().apply { put(executionData) }, cb).analyzeClass(
            initialClassBytes[executionData.name],
            executionData.name
        )
        return cb.getBundle("").packages.flatMap { p ->
            listOf(p.coverageKey() to testName) + p.classes.flatMap { c ->
                listOf(c.coverageKey() to testName) + c.methods.flatMap { m ->
                    if (m.instructionCounter.coveredCount > 0) {
                        listOf(m.coverageKey(c) to testName)
                    } else emptyList()
                }
            }
        }
    }

    @Deprecated(message = "Deprecated 4/17/19")
    private fun classCoverage(
        bundleCoverage: IBundleCoverage,
        assocTestsMap: Map<CoverageKey, List<String>>
    ): List<JavaClassCoverage> = bundleCoverage.packages
        .flatMap { it.classes }
        .let { classCoverage(it, assocTestsMap) }

    private fun packageCoverage(
        bundleCoverage: IBundleCoverage,
        assocTestsMap: Map<CoverageKey, List<String>>
    ): List<JavaPackageCoverage> = bundleCoverage.packages
        .map { packageCoverage ->
            val packageKey = packageCoverage.coverageKey()
            JavaPackageCoverage(
                id = packageKey.id,
                name = packageCoverage.name,
                coverage = packageCoverage.coverage,
                totalClassesCount = packageCoverage.classCounter.totalCount,
                coveredClassesCount = packageCoverage.classCounter.coveredCount,
                totalMethodsCount = packageCoverage.methodCounter.totalCount,
                coveredMethodsCount = packageCoverage.methodCounter.coveredCount,
                assocTestsCount = assocTestsMap[packageKey]?.count(),
                classes = classCoverage(packageCoverage.classes, assocTestsMap)
            )
        }.toList()

    private fun classCoverage(
        classCoverages: Collection<IClassCoverage>,
        assocTestsMap: Map<CoverageKey, List<String>>
    ): List<JavaClassCoverage> = classCoverages
        .map { classCoverage ->
            val classKey = classCoverage.coverageKey()
            JavaClassCoverage(
                id = classKey.id,
                name = classCoverage.name.substringAfterLast('/'),
                path = classCoverage.name,
                coverage = classCoverage.coverage,
                totalMethodsCount = classCoverage.methodCounter.totalCount,
                coveredMethodsCount = classCoverage.methodCounter.coveredCount,
                assocTestsCount = assocTestsMap[classKey]?.count(),
                methods = classCoverage.methods.map { methodCoverage ->
                    val methodKey = methodCoverage.coverageKey(classCoverage)
                    JavaMethodCoverage(
                        id = methodKey.id,
                        name = methodCoverage.name,
                        desc = methodCoverage.desc,
                        coverage = methodCoverage.coverage,
                        assocTestsCount = assocTestsMap[methodKey]?.count()
                    )
                }.toList()
            )
        }.toList()

    private fun methodCoverageId(
        classCoverage: IClassCoverage,
        methodCoverage: IMethodCoverage
    ) = "${classCoverage.name}.${methodCoverage.name}${methodCoverage.desc}".crc64
}

