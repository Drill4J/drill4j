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
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.NewObject

@Suppress("unused")
class CoverageController(private val ws: WsService, val name: String) : AdminPluginPart(ws, name) {

    val initialClassBytes = mutableMapOf<String, ByteArray>()

    val javaClasses = mutableMapOf<String, JavaClass>()

    //TODO Only the last prev state at this moment - use JaVers repositories
    val prevJavaClasses = mutableMapOf<String, JavaClass>()

    private val javers = JaversBuilder.javers().build()

    override suspend fun processData(agentInfo: AgentInfo, dm: DrillMessage): Any {
        val sessionId = dm.sessionId
        val content = dm.content
        val parse = JSON.parse(CoverageMessage.serializer(), content!!)
        when (parse.type) {
            CoverageEventType.INIT -> {
                println(parse.data) //log init message
                //change maps
                initialClassBytes.clear()
                prevJavaClasses.clear()
                prevJavaClasses.putAll(javaClasses)
                javaClasses.clear()
            }
            CoverageEventType.CLASS_BYTES -> {
                val classData = JSON.parse(ClassBytes.serializer(), parse.data)
                val className = classData.className
                val bytes = classData.bytes.toByteArray()
                initialClassBytes[className] = bytes
            }
            CoverageEventType.INITIALIZED -> {
                println(parse.data) //log initialized message
                val coverageBuilder = CoverageBuilder()
                val analyzer = Analyzer(ExecutionDataStore(), coverageBuilder)
                initialClassBytes.forEach { (name, bytes) ->
                    analyzer.analyzeClass(bytes, name)
                }
                val bundleCoverage = coverageBuilder.getBundle("all")
                fillJavaClasses(bundleCoverage)
                println("Classes loaded ${initialClassBytes.count()}")
            }
            CoverageEventType.COVERAGE_DATA -> {
                val coverageBuilder = CoverageBuilder()
                val dataStore = ExecutionDataStore()
                val analyzer = Analyzer(dataStore, coverageBuilder)

                // Get new probes from message and populate dataStore with them
                //also fill up assoc tests
                val probes = JSON.parse(ExDataTemp.serializer().list, parse.data)
                val assocTests = probes.flatMap { exData ->
                    if (exData.testName != null) println("${exData.className} ---- ${exData.testName}")
                    val executionData = ExecutionData(exData.id, exData.className, exData.probes.toBooleanArray())
                    dataStore.put(executionData)
                    when (exData.testName) {
                        null -> emptyList()
                        else -> collectAssocTestPairs(executionData, exData.testName)
                    }
                }.groupBy({ it.first }) { it.second } //group by test names
                    .map { (id, tests) -> AssociatedTests(id = id, tests = tests.distinct()) }
                if (assocTests.isNotEmpty()) {
                    println("Assoc tests - ids count: ${assocTests.count()}")
                    println(assocTests)
                    ws.convertAndSend(
                        agentInfo,
                        "/associated-tests",
                        JSON.stringify(AssociatedTests.serializer().list, assocTests)
                    )
                }

                // Analyze all existing classes
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

                val classCoverage = classCoverage(bundleCoverage)
                println(classCoverage)
                ws.convertAndSend(
                    agentInfo,
                    "/coverage-by-classes",
                    JSON.stringify(JavaClassCoverage.serializer().list, classCoverage)
                )

                val packageCoverage = packageCoverage(bundleCoverage)
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

    private fun collectAssocTestPairs(
        executionData: ExecutionData,
        testName: String
    ): List<Pair<String, String>> {
        val cb = CoverageBuilder()
        Analyzer(ExecutionDataStore().apply { put(executionData) }, cb).analyzeClass(
            initialClassBytes[executionData.name],
            executionData.name
        )
        return cb.getBundle("").packages.flatMap { p ->
            listOf(p.name.crc64 to testName) + p.classes.flatMap { c ->
                listOf(c.name.crc64 to testName) + c.methods.flatMap { m ->
                    listOf(methodCoverageId(c, m) to testName)
                }
            }
        }
    }

    @Deprecated(message="Deprecated 4/17/19")
    private fun classCoverage(bundleCoverage: IBundleCoverage): List<JavaClassCoverage> = bundleCoverage.packages
        .flatMap { it.classes }
        .let { classCoverage(it) }

    private fun packageCoverage(bundleCoverage: IBundleCoverage): List<JavaPackageCoverage> = bundleCoverage.packages
        .map { packageCoverage ->
            JavaPackageCoverage(
                id = packageCoverage.name.crc64,
                name = packageCoverage.name,
                coverage = packageCoverage.coverage,
                totalClassesCount = packageCoverage.classCounter.totalCount,
                coveredClassesCount = packageCoverage.classCounter.coveredCount,
                totalMethodsCount = packageCoverage.methodCounter.totalCount,
                coveredMethodsCount = packageCoverage.methodCounter.coveredCount,
                classes = classCoverage(packageCoverage.classes)
            )
        }.toList()

    private fun classCoverage(classCoverages: Collection<IClassCoverage>): List<JavaClassCoverage> = classCoverages
        .map { classCoverage ->
            JavaClassCoverage(
                id = classCoverage.name.crc64,
                name = classCoverage.name.substringAfterLast('/'),
                path = classCoverage.name,
                coverage = classCoverage.coverage,
                totalMethodsCount = classCoverage.methodCounter.totalCount,
                coveredMethodsCount = classCoverage.methodCounter.coveredCount,
                methods = classCoverage.methods.map { methodCoverage ->
                    JavaMethodCoverage(
                        id = methodCoverageId(classCoverage, methodCoverage),
                        name = methodCoverage.name,
                        desc = methodCoverage.desc,
                        coverage = methodCoverage.coverage
                    )
                }.toList()
            )
        }.toList()

    private fun methodCoverageId(
        classCoverage: IClassCoverage,
        methodCoverage: IMethodCoverage
    ) = "${classCoverage.name}.${methodCoverage.name}${methodCoverage.desc}".crc64
}

