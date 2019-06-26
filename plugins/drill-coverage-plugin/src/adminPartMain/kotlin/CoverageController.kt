package com.epam.drill.plugins.coverage


import com.epam.drill.common.AgentInfo
import com.epam.drill.common.emptyAgentInfo
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
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

internal val agentStates = ConcurrentHashMap<String, AgentState>()

@Suppress("unused")
class CoverageController(private val ws: WsService, id: String) : AdminPluginPart<Action>(ws, id) {
    override var actionSerializer: kotlinx.serialization.KSerializer<Action> = Action.serializer()
    private var scopeName: String = ""
    private var agentInfo: AgentInfo = emptyAgentInfo()

    override suspend fun doAction(pAgentInfo: AgentInfo, action: Action) {
        agentInfo = pAgentInfo
        val agentState = getAgentStateByAgentInfo()
        when (action.type) {
            ActionType.CREATE_SCOPE -> {
                scopeName = action.payload.scopeName
                updateScopeData()
                val scopeProbes = defineProbesForScope(agentState)
                val cis = calculateCoverageData(agentState, scopeProbes)
                deliverMessages(cis)
            }
            ActionType.CLOSE_SCOPE -> {
                scopeName = ""
                updateScopeData()
                val buildProbes = defineProbesForBuild()
                val cis = calculateCoverageData(agentState, buildProbes)
                deliverMessages(cis, "") //"build-")
            }
            ActionType.DROP_SCOPE -> {
                scopeName = ""
                val scopeProbes = defineProbesForScope(agentState)
                calculateCoverageData(agentState, scopeProbes)
            }
            ActionType.MANAGE_SCOPE -> {
                val scope = getScopeOrNull()
                when (scope) {
                    null -> Unit
                    else -> scope.accounted = !scope.accounted
                }
            }
            else -> Unit
        }
    }

    override suspend fun processData(pAgentInfo: AgentInfo, dm: DrillMessage): Any {
        agentInfo = pAgentInfo
        val agentState = getAgentStateByAgentInfo()
        val content = dm.content
        val message = CoverageMessage.serializer() parse content!!
        return processData(agentState, message)
    }

    private fun getAgentStateByAgentInfo() =
        agentStates.compute(agentInfo.id) { _, state ->
            when (state?.agentInfo) {
                agentInfo -> state
                else -> AgentState(agentInfo, state)
            }
        }!!

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun processData(agentState: AgentState, parse: CoverageMessage): Any {
        when (parse.type) {
            CoverageEventType.INIT -> {
                updateScopeData()
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
                updateGatheringState(true)
            }
            CoverageEventType.SESSION_CANCELLED -> {
                val classesData = agentState.classesData()
                classesData.execData.stop()
                println("Session ${parse.data} cancelled.")
                updateGatheringState(false)
            }
            CoverageEventType.COVERAGE_DATA_PART -> {
                val classesData = agentState.classesData()
                val probes = ExDataTemp.serializer().list parse parse.data
                probes.forEach {
                    classesData.execData.add(it)
                }
            }
            CoverageEventType.SESSION_FINISHED -> {
                val scopeProbes = defineProbesForScope(agentState)
                val cis = calculateCoverageData(agentState, scopeProbes)
                deliverMessages(cis)
            }
        }
        return ""
    }

    fun defineProbesForScope(
        agentState: AgentState
    ): Set<ExDataTemp> {
        val scope = authenticateScope()
        val classesData = agentState.classesData()
        scope.probes.addAll(classesData.execData.stop())
        return scope.probes
    }

    fun defineProbesForBuild(): Set<ExDataTemp> {
        val buildScopeKeys = getKeysForBuildScopes()
        val accountedScopes = buildScopeKeys.map { key ->
            getScopeOrNull(key)
        }.filter { it?.accounted ?: false }
        return accountedScopes.flatMap { it?.probes ?: mutableSetOf() }.toSet()
    }

    suspend fun calculateCoverageData(
        agentState: AgentState,
        scopeProbes: Set<ExDataTemp>
    ): CoverageInfoSet {
        val classesData = agentState.classesData()
        updateGatheringState(false)
        // Analyze all existing classes
        val coverageBuilder = CoverageBuilder()
        val dataStore = ExecutionDataStore()
        val initialClassBytes = classesData.classesBytes
        val analyzer = Analyzer(dataStore, coverageBuilder)
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
        val associatedTests = assocTestsMap.map { (key, tests) ->
            AssociatedTests(
                id = key.id,
                packageName = key.packageName,
                className = key.className,
                methodName = key.methodName,
                tests = tests
            )
        }

        initialClassBytes.forEach { (name, bytes) ->
            analyzer.analyzeClass(bytes, name)
        }

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
        val packageCoverage = packageCoverage(bundleCoverage, assocTestsMap)
        val testRelatedBundles = testUsageBundles(initialClassBytes, scopeProbes)
        val testUsages = testUsages(testRelatedBundles)
        return CoverageInfoSet(
            associatedTests,
            coverageBlock,
            newCoverageBlock,
            newMethodsCoverages,
            packageCoverage,
            testUsages
        )
    }

    fun getScopeOrNull(
        storageKey: String = "${agentInfo.id}-${agentInfo.buildVersion}-$scopeName"
    ): Scope? {
        val scopeUncasted = ws.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        return when (scopeUncasted) {
            null -> null
            else -> scopeUncasted as Scope
        }
    }

    fun authenticateScope(): Scope {
        val scope = getScopeOrNull()
        return when (scope) {
            null -> {
                val storeData = Scope(scopeName)
                ws.storeData("${agentInfo.id}-${agentInfo.buildVersion}-$scopeName", storeData)
                return storeData
            }
            else -> scope
        }
    }

    fun getKeysForBuildScopes(): List<String> {
        val storageKeyPrefix = "${agentInfo.id}-${agentInfo.buildVersion}"
        return ws.retrieveKeysByPrefix(storageKeyPrefix)
    }

    suspend fun deliverMessages(cis: CoverageInfoSet, prefix: String = "") {
        // TODO extend destination with plugin id
        if (cis.associatedTests.isNotEmpty()) {
            println("Assoc tests - ids count: ${cis.associatedTests.count()}")
            ws.convertAndSend(
                agentInfo,
                "/${prefix}associated-tests",
                AssociatedTests.serializer().list stringify cis.associatedTests
            )
        }
        ws.convertAndSend(
            agentInfo,
            "/${prefix}coverage",
            CoverageBlock.serializer() stringify cis.coverageBlock
        )
        ws.convertAndSend(
            agentInfo,
            "/${prefix}coverage-new",
            NewCoverageBlock.serializer() stringify cis.newCoverageBlock
        )
        ws.convertAndSend(
            agentInfo,
            "/${prefix}new-methods",
            SimpleJavaMethodCoverage.serializer().list stringify cis.newMethodsCoverages
        )
        ws.convertAndSend(
            agentInfo,
            "/${prefix}coverage-by-packages",
            JavaPackageCoverage.serializer().list stringify cis.packageCoverage
        )
        ws.convertAndSend(
            agentInfo,
            "/${prefix}tests-usages",
            TestUsagesInfo.serializer().list stringify cis.testUsages
        )
    }

    suspend fun updateScopeData() {
        val storageKey = "scopes:${agentInfo.id}:${agentInfo.buildVersion}"
        val scopesUncasted = ws.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        val scopes =
            if (scopesUncasted == null) mutableSetOf()
            else scopesUncasted as MutableSet<String>
        scopes.add(scopeName)
        ws.storeData(storageKey, scopes)
        updateScope()
        updateScopesSet(scopes)
    }

    private suspend fun updateGatheringState(state: Boolean) {
        ws.convertAndSend(
            agentInfo,
            "/collection-state",
            GatheringState.serializer() stringify GatheringState(state)
        )
    }

    private suspend fun updateScope() {
        val accounted = getScopeOrNull()?.accounted ?: false
        ws.convertAndSend(
            agentInfo,
            "/active-scope",
            ActiveScope.serializer() stringify ActiveScope(scopeName, accounted)
        )
    }

    private suspend fun updateScopesSet(scopes: Set<String>) {
        ws.convertAndSend(
            agentInfo,
            "/scopes",
            String.serializer().set stringify scopes
        )
    }


}