package com.epam.drill.plugins.coverage


import com.epam.drill.common.AgentInfo
import com.epam.drill.common.parse
import com.epam.drill.common.stringify
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import kotlinx.serialization.set
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.IMethodCoverage
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

internal val agentStates = ConcurrentHashMap<String, AgentState>()

@Suppress("unused")
class CoverageController(private val ws: WsService, id: String) : AdminPluginPart<Action>(ws, id) {
    override var actionSerializer: kotlinx.serialization.KSerializer<Action> = Action.serializer()
    private var scope: String = ""

    override suspend fun doAction(agentInfo: AgentInfo, action: Action) {
        val agentState = getAgentStateBy(agentInfo)
        when (action.type) {
            ActionType.CREATE_SCOPE -> {
                scope = action.payload.scopeName
                updateScopeData(agentInfo)
                calculateCoverageData(agentInfo, agentState)
            }
            ActionType.DROP_SCOPE -> {
                scope = ""
                calculateCoverageData(agentInfo, agentState)
            }
            else -> Unit
        }
    }

    override suspend fun processData(agentInfo: AgentInfo, dm: DrillMessage): Any {
        val agentState = getAgentStateBy(agentInfo)
        val content = dm.content
        val message = CoverageMessage.serializer() parse content!!
        return processData(agentState, message)
    }

    private fun getAgentStateBy(agentInfo: AgentInfo) =
        agentStates.compute(agentInfo.id) { _, state ->
            when (state?.agentInfo) {
                agentInfo -> state
                else -> AgentState(agentInfo, state)
            }
        }!!

    @Suppress("MemberVisibilityCanBePrivate")// debug problem with private modifier
    suspend fun processData(agentState: AgentState, parse: CoverageMessage): Any {
        val agentInfo = agentState.agentInfo
        when (parse.type) {
            CoverageEventType.INIT -> {
                updateScopeData(agentInfo)
                val initInfo = InitInfo.serializer() parse parse.data
                agentState.init(initInfo)
                println(initInfo.message) //log init message
                println("${initInfo.classesCount} classes to load")
            }
            CoverageEventType.CLASS_BYTES -> {
                val classData = ClassBytes.serializer() parse parse.data
                val className = classData.className
                val bytes = classData.bytes.toByteArray()
                agentState.addClass(className, bytes)
            }
            CoverageEventType.INITIALIZED -> {
                println(parse.data) //log initialized message
                agentState.initialized()
                val classesData = agentState.classesData()
                if (classesData.changed) {
                    classesData.execData.start()
                    processData(agentState, CoverageMessage(CoverageEventType.SESSION_FINISHED, ""))
                }
            }
            CoverageEventType.SESSION_STARTED -> {
                val classesData = agentState.classesData()
                classesData.execData.start()
                println("Session ${parse.data} started.")
                updateGatheringState(agentInfo, true)
            }
            CoverageEventType.SESSION_CANCELLED -> {
                val classesData = agentState.classesData()
                classesData.execData.stop()
                println("Session ${parse.data} cancelled.")
                updateGatheringState(agentInfo, false)
            }
            CoverageEventType.COVERAGE_DATA_PART -> {
                val classesData = agentState.classesData()
                val probes = ExDataTemp.serializer().list parse parse.data
                probes.forEach {
                    classesData.execData.add(it)
                }
            }
            CoverageEventType.SESSION_FINISHED -> {
                calculateCoverageData(agentInfo, agentState)
            }
        }
        return ""
    }

    suspend fun calculateCoverageData(
        agentInfo: AgentInfo,
        agentState: AgentState
    ) {
        updateGatheringState(agentInfo, false)
        // Analyze all existing classes
        val classesData = agentState.classesData()
        val initialClassBytes = classesData.classesBytes

        val coverageBuilder = CoverageBuilder()
        val dataStore = ExecutionDataStore()
        val analyzer = Analyzer(dataStore, coverageBuilder)

        // Get new probes from message and populate dataStore with them
        //also fill up assoc tests
        val probesForMerge = classesData.execData.stop()
        val storageKey = "${agentInfo.id}-${agentInfo.buildVersion}-$scope"
        val scopeProbesUncasted = ws.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        val scopeProbes =
            if (scopeProbesUncasted == null) mutableSetOf()
            else scopeProbesUncasted as MutableSet<ExDataTemp>
        scopeProbes.addAll(probesForMerge)
        ws.storeData(storageKey, scopeProbes)

        val assocTestsMap = scopeProbes.flatMap { exData ->
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
            ws.convertAndSend(
                agentInfo,
                "/associated-tests",
                AssociatedTests.serializer().list stringify assocTests
            )
        }


        initialClassBytes.forEach { (name, bytes) ->
            analyzer.analyzeClass(bytes, name)
        }

        // TODO possible to store existing bundles to work with obsolete coverage results
        val bundleCoverage = coverageBuilder.getBundle("all")

        val totalCoveragePercent = bundleCoverage.coverage
        // change arrow indicator (increase, decrease)
        val arrow = if (totalCoveragePercent != null) {
            val prevCoverage = classesData.execData.coverage ?: 0.0
            classesData.execData.coverage = totalCoveragePercent
            val diff = totalCoveragePercent - prevCoverage
            when {
                abs(diff) < 1E-7 -> null
                diff > 0.0 -> ArrowType.INCREASE
                else -> ArrowType.DECREASE
            }
        } else null

        classesData.execData.coverage = totalCoveragePercent


        val classesCount = bundleCoverage.classCounter.totalCount
        val methodsCount = bundleCoverage.methodCounter.totalCount
        val uncoveredMethodsCount = bundleCoverage.methodCounter.missedCount

        val coverageBlock = CoverageBlock(
            coverage = totalCoveragePercent,
            classesCount = classesCount,
            methodsCount = methodsCount,
            uncoveredMethodsCount = uncoveredMethodsCount,
            arrow = arrow
        )
        println(coverageBlock)
        ws.convertAndSend(
            agentInfo,
            "/coverage",
            CoverageBlock.serializer() stringify coverageBlock
        )

        val newMethods = classesData.newMethods
        val (newCoverageBlock, newMethodsCoverages) = if (newMethods.isNotEmpty()) {
            println("New methods count: ${newMethods.count()}")
            val newMethodSet = newMethods.toSet()
            val newMethodsCoverages = bundleCoverage.packages
                .flatMap { it.classes }
                .flatMap { c -> c.methods.map { Pair(JavaMethod(c.name, it.name, it.desc), it) } }
                .filter { it.first in newMethodSet }
            val totalCount = newMethodsCoverages.sumBy { it.second.instructionCounter.totalCount }
            val coveredCount = newMethodsCoverages.sumBy { it.second.instructionCounter.coveredCount }
            //bytecode instruction coverage
            val newCoverage = if (totalCount > 0) coveredCount.toDouble() / totalCount * 100 else null

            val coverages = newMethodsCoverages.map { (jm, mc) -> mc.simpleMethodCoverage(jm.ownerClass) }
            NewCoverageBlock(
                methodsCount = newMethodsCoverages.count(),
                methodsCovered = newMethodsCoverages.count { it.second.methodCounter.coveredCount > 0 },
                coverage = newCoverage
            ) to coverages
        } else NewCoverageBlock() to emptyList()
        println(newCoverageBlock)

        // TODO extend destination with plugin id
        ws.convertAndSend(
            agentInfo,
            "/coverage-new",
            NewCoverageBlock.serializer() stringify newCoverageBlock
        )

        ws.convertAndSend(
            agentInfo,
            "/new-methods",
            SimpleJavaMethodCoverage.serializer().list stringify newMethodsCoverages
        )

        val packageCoverage = packageCoverage(bundleCoverage, assocTestsMap)
        ws.convertAndSend(
            agentInfo,
            "/coverage-by-packages",
            JavaPackageCoverage.serializer().list stringify packageCoverage
        )
        val testRelatedBundles = testUsageBundles(initialClassBytes, scopeProbes)
        val testUsages = testUsages(testRelatedBundles)
        ws.convertAndSend(
            agentInfo,
            "/tests-usages",
            TestUsagesInfo.serializer().list stringify testUsages
        )
    }

    suspend fun updateScopeData(agentInfo: AgentInfo){
        val storageKey = "scopes:${agentInfo.id}:${agentInfo.buildVersion}"
        val scopesUncasted = ws.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        val scopes =
            if (scopesUncasted == null) mutableSetOf()
            else scopesUncasted as MutableSet<String>
        scopes.add(scope)
        ws.storeData(storageKey, scopes)
        updateScope(agentInfo, scope)
        updateScopesSet(agentInfo, scopes)
    }

    private suspend fun updateGatheringState(agentInfo: AgentInfo, state: Boolean) {
        ws.convertAndSend(
            agentInfo,
            "/collection-state",
            GatheringState.serializer() stringify GatheringState(state)
        )
    }

    private suspend fun updateScope(agentInfo: AgentInfo, scope: String) {
        ws.convertAndSend(
            agentInfo,
            "/active-scope",
            scope
        )
    }

    private suspend fun updateScopesSet(agentInfo: AgentInfo, scopes: Set<String>) {
        ws.convertAndSend(
            agentInfo,
            "/scopes",
            String.serializer().set stringify scopes
        )
    }

    private fun testUsages(bundleMap: Map<String, IBundleCoverage>): List<TestUsagesInfo> =
        bundleMap.map { (k, v) ->
            TestUsagesInfo(k, v.methodCounter.coveredCount, "Test type", "30.02.2019")
        }

    private fun testUsageBundles(
        initialClassBytes: Map<String, ByteArray>,
        probes: Collection<ExDataTemp>
    ): Map<String, IBundleCoverage> = probes
        .filter { it.testName != null }
        .groupBy { it.testName!! }
        .mapValues { (_, v) ->
            val dataStore = ExecutionDataStore()
            v.forEach {
                val probeArray = it.probes.toBooleanArray()
                val executionData = ExecutionData(it.id, it.className, probeArray)
                dataStore.put(executionData)
            }
            testUsageBundle(initialClassBytes, dataStore)
        }

    private fun testUsageBundle(
        initialClassBytes: Map<String, ByteArray>,
        dataStore: ExecutionDataStore
    ): IBundleCoverage {
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(dataStore, coverageBuilder)
        dataStore.contents.forEach { execData ->
            analyzer.analyzeClass(initialClassBytes[execData.name], execData.name)
        }
        return coverageBuilder.getBundle("all")
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
                        decl = declaration(methodCoverage.desc),
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
