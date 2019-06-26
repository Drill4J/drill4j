package com.epam.drill.plugins.coverage


import com.epam.drill.common.AgentInfo
import com.epam.drill.common.emptyAgentInfo
import com.epam.drill.common.parse
import com.epam.drill.common.stringify
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.list
import kotlinx.serialization.set
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.data.ExecutionDataStore
import java.util.concurrent.ConcurrentHashMap

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
                checkoutScope(action.payload.scopeName, agentState)
            }
            ActionType.CHANGE_SCOPE -> {
                checkoutScope(action.payload.scopeName, agentState)
            }
            ActionType.CLOSE_SCOPE -> {
                scopeName = ""
                updateScope()
                val buildProbes = defineProbesForBuild()
                val cis = calculateCoverageData(agentState, buildProbes)
                deliverMessages(cis, "build-")
            }
            ActionType.DROP_SCOPE -> {
                scopeName = ""
                val scopeProbes = defineProbesForScope(agentState)
                calculateCoverageData(agentState, scopeProbes)
            }
            ActionType.TOGGLE_SCOPE -> {
                val scope = getScopeOrNull(
                    "${agentInfo.id}-${agentInfo.buildVersion}-${action.payload.scopeName}"
                )
                when (scope) {
                    null -> Unit
                    else -> scope.accounted = !scope.accounted
                }
            }
            else -> Unit
        }
    }

    private suspend fun checkoutScope(newScopeName: String, agentState: AgentState) {
        scopeName = newScopeName
        updateScope()
        val scopeProbes = defineProbesForScope(agentState)
        val cis = calculateCoverageData(agentState, scopeProbes)
        deliverMessages(cis)
        updateScopeData()
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
                updateScope()
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
        val assocTestsMap = getAssociatedTestMap(scopeProbes, dataStore, initialClassBytes)
        val associatedTests = assocTestsMap.getAssociatedTests()

        initialClassBytes.forEach { (name, bytes) ->
            analyzer.analyzeClass(bytes, name)
        }

        val bundleCoverage = coverageBuilder.getBundle("all")
        val totalCoveragePercent = bundleCoverage.coverage
        // change arrow indicator (increase, decrease)
        val arrow = arrowType(totalCoveragePercent, classesData)

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
        val (newCoverageBlock, newMethodsCoverages)
                = calculateNewCoverageBlock(newMethods, bundleCoverage)
        println(newCoverageBlock)

        val packageCoverage = packageCoverage(bundleCoverage, assocTestsMap)
        val testRelatedBundles = testUsageBundles(initialClassBytes, scopeProbes)
        val testUsages = testUsages(testRelatedBundles)
        val scopeInfosByTestType = infosByTestType(initialClassBytes, scopeProbes)
        updateScopeData(totalCoveragePercent, scopeInfosByTestType)

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

    suspend fun updateScopeData(scopeCoverage: Double? = null, infosByTestType: List<InfoByTestType>? = null) {
        val storageKey = "scope:${agentInfo.id}:${agentInfo.buildVersion}"
        val scopesUncasted = ws.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        val scopes =
            if (scopesUncasted == null) {
                val emptySet = mutableSetOf<ScopeInfo>()
                ws.storeData(storageKey, emptySet)
                emptySet
            } else scopesUncasted as MutableSet<ScopeInfo>
        val accounted = getScopeOrNull()?.accounted ?: true
        val scope = scopes.find { it.name == scopeName }
            ?: ScopeInfo(scopeName, accounted).apply { scopes.add(this) }
        if (scopeCoverage != null) scope.coverage = scopeCoverage
        if (infosByTestType != null) scope.infosByTestType = infosByTestType
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
        ws.convertAndSend(
            agentInfo,
            "/active-scope",
            scopeName
        )
    }

    private suspend fun updateScopesSet(scopes: Set<ScopeInfo>) {
        ws.convertAndSend(
            agentInfo,
            "/scopes",
            ScopeInfo.serializer().set stringify scopes
        )
    }

}