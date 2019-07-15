package com.epam.drill.plugins.coverage


import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import kotlinx.serialization.*
import org.jacoco.core.analysis.*
import org.jacoco.core.data.*

internal val agentStates = AtomicCache<String, AgentState>()

//TODO This is a temporary storage API. It will be removed when the core API has been developed
private val agentStorages = AtomicCache<String, Storage>()

@Suppress("unused", "MemberVisibilityCanBePrivate")
class CoverageAdminPart(sender: Sender, agentInfo: AgentInfo, id: String) :
    AdminPluginPart<Action>(sender, agentInfo, id) {

    override val serDe: SerDe<Action> = commonSerDe

    private val buildVersion = agentInfo.buildVersion

    //TODO This is a temporary storage API. It will be removed when the core API has been developed
    private val storage: Storage get() = agentStorages.getOrPut(agentInfo.id) { MapStorage() }

    private val agentState: AgentState = agentStates(agentInfo.id) { state ->
        when (state?.agentInfo) {
            agentInfo -> state
            else -> AgentState(agentInfo, state)
        }
    }!!

    private val scopesKey = ScopesKey(buildVersion)

    override suspend fun doAction(action: Action): Any {
        return when (action) {
            is SwitchScope -> checkoutScope(action.payload.scopeName)
            is IgnoreScope -> toggleScope(action.payload.scopeId, action.payload.enabled)
            is DropScope -> dropScope(action.payload.scopeName)
            is StartNewSession -> {
                val startAgentSession = StartSession(
                    payload = StartSessionPayload(
                        sessionId = genUuid(),
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
        return processData(message)
    }

    internal suspend fun processData(coverMsg: CoverMessage): Any {
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
                    //TODO send package tree
                }
            }
            is SessionStarted -> {
                agentState.startSession(coverMsg)
                println("Session ${coverMsg.sessionId} started.")
                updateGatheringState(true)
            }
            is SessionCancelled -> {
                agentState.cancelSession(coverMsg)
                println("Session ${coverMsg.sessionId} cancelled.")
                updateGatheringState(false)
            }
            is CoverDataPart -> {
                agentState.addProbes(coverMsg)
            }
            is SessionFinished -> {
                val scope = agentState.activeScope
                when(val session = agentState.finishSession(coverMsg)) {
                    null -> println("No active session for sessionId ${coverMsg.sessionId}")
                    else -> {
                        if (session.any()) {
                            scope.append(session)
                        } else println("Session ${session.id} is empty, it won't be added to the active scope")
                        val cis = calculateCoverageData(scope)
                        updateGatheringState(false)
                        sendCalcResults(cis)
                        println("Session ${session.id} finished.")
                    }
                }
            }
        }
        return ""
    }

    internal fun calculateCoverageData(scope: ActiveScope): CoverageInfoSet {
        val classesData = agentState.classesData()
        // Analyze all existing classes
        val coverageBuilder = CoverageBuilder()
        val dataStore = ExecutionDataStore()
        val initialClassBytes = classesData.classesBytes
        val analyzer = Analyzer(dataStore, coverageBuilder)

        val scopeProbes = scope.toList()
        val assocTestsMap = getAssociatedTestMap(scopeProbes, dataStore, initialClassBytes)
        val associatedTests = assocTestsMap.getAssociatedTests()

        initialClassBytes.forEach { (name, bytes) ->
            analyzer.analyzeClass(bytes, name)
        }
        val bundleCoverage = coverageBuilder.getBundle("all")
        val totalCoveragePercent =
            bundleCoverage.instructionCounter.coveredCount * 100.0  / classesData.instructionsCount
        // change arrow indicator (increase, decrease)
        val arrow = arrowType(totalCoveragePercent, scope)
        scope.lastCoverage = totalCoveragePercent

        val classesCount = classesData.classesCount
        val methodsCount = classesData.methodsCount
        val uncoveredMethodsCount = methodsCount - bundleCoverage.methodCounter.coveredCount
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
            agentState.activeScope.name
        )
    }

    internal suspend fun sendScopes(scopes: Scopes) {
        sender.send(
            agentInfo,
            "/scopes",
            String.serializer().set stringify scopes.keys
        )
    }

    internal suspend fun toggleScope(scopeId: String, enabled: Boolean) {
        val toggleScopeKey = ScopeKey(agentInfo.buildVersion, scopeId)
        val scope = storage.retrieve(toggleScopeKey)
        scope?.enabled = enabled
    }

    @Suppress("UNUSED_PARAMETER")
    internal suspend fun dropScope(scopeName: String) {
        //todo drop scope
        checkoutScope("")
    }

    private suspend fun checkoutScope(scopeName: String) {
        when (@Suppress("UNUSED_VARIABLE") val finishedScope = agentState.changeScope(scopeName)) {
            null -> Unit
            else -> {
                //TODO Finished Scope storage
                val cis = calculateCoverageData(agentState.activeScope)
                sendCalcResults(cis)
            }
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