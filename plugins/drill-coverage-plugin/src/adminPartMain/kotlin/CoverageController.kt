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
import org.jacoco.core.data.ExecutionDataStore
import java.util.concurrent.ConcurrentHashMap

internal val agentStates = ConcurrentHashMap<String, AgentState>()

@Suppress("unused")
class CoverageController(sender: WsService, agentInfo: AgentInfo, id: String) :
    AdminPluginPart<Action>(sender, agentInfo, id) {
    override var actionSerializer: kotlinx.serialization.KSerializer<Action> = Action.serializer()
    private var scopeName: String = ""

    override suspend fun doAction(action: Action) {
        val agentState = getAgentStateByAgentInfo()
        when (action.type) {
            ActionType.CREATE_SCOPE -> {
                checkoutScope(action.payload.scopeName, agentState)
            }
            ActionType.DROP_SCOPE -> {
                checkoutScope("", agentState)
            }
            else -> Unit
        }
    }

    override suspend fun processData(dm: DrillMessage): Any {
        val agentState = getAgentStateByAgentInfo()
        val content = dm.content
        val message = CoverageMessage.serializer() parse content!!
        return processData(agentState, message)
    }

    fun getAgentStateByAgentInfo() =
        agentStates.compute(agentInfo.id) { _, state ->
            when (state?.agentInfo) {
                agentInfo -> state
                else -> AgentState(agentInfo, state)
            }
        }!!

    @Suppress("MemberVisibilityCanBePrivate")// debug problem with private modifier
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
                updateGatheringState(false)
                val scopeProbes = defineProbesForScope(agentState)
                val cis = calculateCoverageData(agentState, scopeProbes)
                deliverMessages(cis)
            }
        }
        return ""
    }

    suspend fun calculateCoverageData(
        agentState: AgentState,
        scopeProbes: List<ExDataTemp>
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

        return CoverageInfoSet(
            associatedTests,
            coverageBlock,
            newCoverageBlock,
            newMethodsCoverages,
            packageCoverage,
            testUsages
        )
    }

    suspend fun updateScopeData() {
        val storageKey = "scopes:${agentInfo.id}:${agentInfo.buildVersion}"
        val scopesUncasted = sender.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        val scopes =
            if (scopesUncasted == null) mutableSetOf()
            else scopesUncasted as MutableSet<String>
        scopes.add(scopeName)
        sender.storeData(storageKey, scopes)
        updateScope()
        updateScopesSet(scopes)
    }

    private suspend fun updateGatheringState(state: Boolean) {
        sender.convertAndSend(
            agentInfo,
            "/collection-state",
            GatheringState.serializer() stringify GatheringState(state)
        )
    }

    private suspend fun updateScope() {
        sender.convertAndSend(
            agentInfo,
            "/active-scope",
            scopeName
        )
    }

    private suspend fun updateScopesSet(scopes: Set<String>) {
        sender.convertAndSend(
            agentInfo,
            "/scopes",
            String.serializer().set stringify scopes
        )
    }

    fun defineProbesForScope(agentState: AgentState): List<ExDataTemp> {
        val scope = getOrCreateScope()
        val classesData = agentState.classesData()
        scope.probes.addAll(classesData.execData.stop())
        return scope.probes
    }

    fun getScopeOrNull(
        storageKey: String = "${agentInfo.id}-${agentInfo.buildVersion}-$scopeName"
    ): Scope? {
        val scopeUncasted = sender.retrieveData(storageKey)
        @Suppress("UNCHECKED_CAST")
        return when (scopeUncasted) {
            null -> null
            else -> scopeUncasted as Scope
        }
    }

    fun getOrCreateScope(): Scope {
        val scope = getScopeOrNull()
        return when (scope) {
            null -> {
                val storeData = Scope(scopeName)
                sender.storeData("${agentInfo.id}-${agentInfo.buildVersion}-$scopeName", storeData)
                return storeData
            }
            else -> scope
        }
    }

    private suspend fun checkoutScope(newScopeName: String, agentState: AgentState) {
        scopeName = newScopeName
        updateScope()
        updateScopeData()
        val scopeProbes = defineProbesForScope(agentState)
        val cis = calculateCoverageData(agentState, scopeProbes)
        deliverMessages(cis)
    }

    suspend fun deliverMessages(cis: CoverageInfoSet, prefix: String = "") {
        // TODO extend destination with plugin id
        if (cis.associatedTests.isNotEmpty()) {
            println("Assoc tests - ids count: ${cis.associatedTests.count()}")
            sender.convertAndSend(
                agentInfo,
                "/${prefix}associated-tests",
                AssociatedTests.serializer().list stringify cis.associatedTests
            )
        }
        sender.convertAndSend(
            agentInfo,
            "/${prefix}coverage",
            CoverageBlock.serializer() stringify cis.coverageBlock
        )
        sender.convertAndSend(
            agentInfo,
            "/${prefix}coverage-new",
            NewCoverageBlock.serializer() stringify cis.newCoverageBlock
        )
        sender.convertAndSend(
            agentInfo,
            "/${prefix}new-methods",
            SimpleJavaMethodCoverage.serializer().list stringify cis.newMethodsCoverages
        )
        sender.convertAndSend(
            agentInfo,
            "/${prefix}coverage-by-packages",
            JavaPackageCoverage.serializer().list stringify cis.packageCoverage
        )
        sender.convertAndSend(
            agentInfo,
            "/${prefix}tests-usages",
            TestUsagesInfo.serializer().list stringify cis.testUsages
        )
    }

}

data class CoverageInfoSet(
    val associatedTests: List<AssociatedTests>,
    val coverageBlock: CoverageBlock,
    val newCoverageBlock: NewCoverageBlock,
    val newMethodsCoverages: List<SimpleJavaMethodCoverage>,
    val packageCoverage: List<JavaPackageCoverage>,
    val testUsages: List<TestUsagesInfo>
)

data class Scope(
    val name: String,
    val probes: MutableList<ExDataTemp> = mutableListOf(),
    var accounted: Boolean = true
)