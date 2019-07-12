package com.epam.drill.plugins.coverage


import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import kotlinx.serialization.*
import org.jacoco.core.analysis.*
import org.jacoco.core.data.*
import java.util.*
import java.util.concurrent.*

internal val agentStates = ConcurrentHashMap<String, AgentState>()

//TODO This is a temporary storage API. It will be removed when the core API has been developed
private val agentStorages = ConcurrentHashMap<String, Storage>()

@Suppress("unused", "MemberVisibilityCanBePrivate")
class CoverageAdminPart(sender: Sender, agentInfo: AgentInfo, id: String) :
    AdminPluginPart<Action>(sender, agentInfo, id) {

    override val serDe: SerDe<Action> = commonSerDe

    //TODO This is a temporary storage API. It will be removed when the core API has been developed
    private val storage: Storage = agentStorages.getOrPut(agentInfo.id) { MapStorage() }

    private val agentState: AgentState = agentStates.compute(agentInfo.id) { _, state ->
        when (state?.agentInfo) {
            agentInfo -> state
            else -> AgentState(agentInfo, state)
        }
    }!!

    @Volatile
    private var scopeKey = ScopeKey(agentInfo.buildVersion, "")

    private val scopesKey = ScopesKey(agentInfo.buildVersion)

    override suspend fun doAction(action: Action): Any {
        return when (action) {
            is SwitchScope -> checkoutScope(action.payload.scopeName)
            is IgnoreScope -> toggleScope(action.payload.scopeName, action.payload.enabled)
            is DropScope -> dropScope(action.payload.scopeName)
            is StartNewSession -> {
                val startAgentSession = StartSession(
                    payload = StartSessionPayload(
                        sessionId = UUID.randomUUID().toString(),
                        startPayload = action.payload
                    )
                )
                serDe.actionSerializer stringify startAgentSession
            }
            else -> Unit
        }
    }

    override suspend fun processData(dm: DrillMessage): Any {
        val content = dm.content
        val message = CoverMessage.serializer() parse content!!
        return processData(dm.sessionId, message)
    }

    internal suspend fun processData(sessionId: String?, coverMsg: CoverMessage): Any {
        when (coverMsg) {
            is InitInfo -> {
                val scopes = storage.retrieve(scopesKey) ?: emptyMap()
                updateScopeMessages(scopes)
                agentState.init(coverMsg)
                println(coverMsg.message) //log init message
                println("${coverMsg.classesCount} classes to load")
            }
            is ClassBytes -> {
                val className = coverMsg.className
                val bytes = coverMsg.bytes.decode()
                agentState.addClass(className, bytes)
            }
            is Initialized -> {
                println(coverMsg.msg) //log initialized message
                agentState.initialized()
                val classesData = agentState.classesData()
                if (classesData.changed) {
                    classesData.execData.start()
                    val defaultScope = Scope()
                    storage.store(ScopeKey(agentInfo.buildVersion), defaultScope)
                    processData(sessionId, SessionFinished(ts = System.currentTimeMillis()))
                }
            }
            is SessionStarted -> {
                val classesData = agentState.classesData()
                classesData.execData.start()
                println("Session $sessionId started.")
                updateGatheringState(true)
            }
            is SessionCancelled -> {
                val classesData = agentState.classesData()
                classesData.execData.stop()
                println("Session $sessionId cancelled.")
                updateGatheringState(false)
            }
            is CoverDataPart -> {
                val classesData = agentState.classesData()
                coverMsg.data.forEach {
                    classesData.execData.add(it)
                }
            }
            is SessionFinished -> {
                val scope = storage.retrieve(scopeKey)!!
                scope.incSessionCount()
                val classesData = agentState.classesData()
                scope.probes.addAll(classesData.execData.stop())
                val cis = calculateCoverageData(scope.probes)
                updateGatheringState(false)
                sendCalcResults(cis)
                println("Session $sessionId finished.")
            }
        }
        return ""
    }

    internal fun calculateCoverageData(scopeProbes: List<ExDataTemp>): CoverageInfoSet {
        val classesData = agentState.classesData()
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

    internal suspend fun updateScopeMessages(scopes: Scopes) {
        sendActiveScopeName()
        sendScopes(scopes)
    }

    internal suspend fun updateGatheringState(state: Boolean) {
        sender.send(
            agentInfo,
            "/collection-state",
            GatheringState.serializer() stringify GatheringState(state)
        )
    }

    internal suspend fun sendActiveScopeName() {
        sender.send(
            agentInfo,
            "/active-scope",
            scopeKey.name
        )
    }

    internal suspend fun sendScopes(scopes: Scopes) {
        sender.send(
            agentInfo,
            "/scopes",
            String.serializer().set stringify scopes.keys
        )
    }

    internal suspend fun toggleScope(scopeName: String, enabled: Boolean) {
        val toggleScopeKey = ScopeKey(agentInfo.buildVersion, scopeName)
        val scope = storage.retrieve(toggleScopeKey)
        scope?.enabled = enabled
    }

    internal suspend fun dropScope(scopeName: String) {
        val currentScopeSet = storage.retrieve(scopesKey) ?: emptyMap()
        if (scopeName in currentScopeSet) {
            val processedScopes = currentScopeSet - scopeName
            storage.store(scopesKey, processedScopes)
            sendScopes(processedScopes)
            val dropScopeKey = ScopeKey(agentInfo.buildVersion, scopeName)
            storage.delete(dropScopeKey)
        }
        checkoutScope("")
    }

    private suspend fun checkoutScope(scopeName: String) {
        if (scopeName != scopeKey.name) {
            val oldScope = storage.retrieve(scopeKey)
            oldScope?.finish()
            scopeKey = ScopeKey(agentInfo.buildVersion, scopeName)
            //TODO Replace this!!!
            val scope = storage.retrieve(scopeKey) ?: Scope().apply { storage.store(scopeKey, this) }
            val scopes = storage.retrieve(scopesKey) ?: emptyMap()
            updateScopeMessages(scopes)

            scope.start()

            val cis = calculateCoverageData(scope.probes)
            sendCalcResults(cis)
        }
    }

    internal suspend fun sendCalcResults(cis: CoverageInfoSet, prefix: String = "") {
        // TODO extend destination with plugin id
        if (cis.associatedTests.isNotEmpty()) {
            println("Assoc tests - ids count: ${cis.associatedTests.count()}")
            sender.send(
                agentInfo,
                "/${prefix}associated-tests",
                AssociatedTests.serializer().list stringify cis.associatedTests
            )
        }
        sender.send(
            agentInfo,
            "/${prefix}coverage",
            CoverageBlock.serializer() stringify cis.coverageBlock
        )
        sender.send(
            agentInfo,
            "/${prefix}coverage-new",
            NewCoverageBlock.serializer() stringify cis.newCoverageBlock
        )
        sender.send(
            agentInfo,
            "/${prefix}new-methods",
            SimpleJavaMethodCoverage.serializer().list stringify cis.newMethodsCoverages
        )
        sender.send(
            agentInfo,
            "/${prefix}coverage-by-packages",
            JavaPackageCoverage.serializer().list stringify cis.packageCoverage
        )
        sender.send(
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