package com.epam.drill.plugins.coverage


import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import kotlinx.serialization.*
import org.jacoco.core.analysis.*
import org.jacoco.core.data.*

internal val agentStates = AtomicCache<String, AgentState>()

@Suppress("unused", "MemberVisibilityCanBePrivate")
class CoverageAdminPart(sender: Sender, agentInfo: AgentInfo, id: String) :
    AdminPluginPart<Action>(sender, agentInfo, id) {

    override val serDe: SerDe<Action> = commonSerDe

    private val buildVersion = agentInfo.buildVersion

    private val agentState: AgentState = agentStates(agentInfo.id) { state ->
        when (state?.agentInfo) {
            agentInfo -> state
            else -> AgentState(agentInfo, state)
        }
    }!!

    override suspend fun doAction(action: Action): Any {
        return when (action) {
            is SwitchActiveScope -> changeActiveScope(action.payload)
            is ToggleScope -> toggleScope(action.payload.scopeId)
            is DropScope -> dropScope(action.payload.scopeId)
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
                sendActiveSessions()
            }
            is SessionCancelled -> {
                agentState.cancelSession(coverMsg)
                println("Session ${coverMsg.sessionId} cancelled.")
                sendActiveSessions()
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
                            val classesData = agentState.classesData()
                            scope.update(session, classesData)
                            sendScopeMessages()
                        } else println("Session ${session.id} is empty, it won't be added to the active scope")
                        val cis = calculateCoverageData(scope)
                        sendActiveSessions()
                        sendCalcResults(cis)
                        println("Session ${session.id} finished.")
                    }
                }
            }
        }
        return ""
    }

    internal fun calculateCoverageData(finishedSessions: Sequence<FinishedSession>): CoverageInfoSet {
        val probes = finishedSessions.flatten()
        val classesData = agentState.classesData()
        // Analyze all existing classes
        val coverageBuilder = CoverageBuilder()
        val dataStore = ExecutionDataStore().with(probes)
        val initialClassBytes = classesData.classesBytes
        val analyzer = Analyzer(dataStore, coverageBuilder)

        val scopeProbes = probes.toList()
        val assocTestsMap = getAssociatedTestMap(scopeProbes, initialClassBytes)
        val associatedTests = assocTestsMap.getAssociatedTests()

        initialClassBytes.forEach { (name, bytes) ->
            analyzer.analyzeClass(bytes, name)
        }
        val bundleCoverage = coverageBuilder.getBundle("")
        val totalCoveragePercent = bundleCoverage.coverage(classesData.totals.instructionCounter.totalCount)

        val classesCount = classesData.totals.classCounter.totalCount
        val methodsCount = classesData.totals.methodCounter.totalCount
        val uncoveredMethodsCount = methodsCount - bundleCoverage.methodCounter.coveredCount
        val coverageBlock = CoverageBlock(
            coverage = totalCoveragePercent,
            classesCount = classesCount,
            methodsCount = methodsCount,
            uncoveredMethodsCount = uncoveredMethodsCount
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

    internal suspend fun sendScopeMessages() {
        sendActiveScope()
        sendScopes()
    }

    internal suspend fun sendActiveSessions() {
        val activeSessions = agentState.activeSessions.run { 
            ActiveSessions(
                count = count(),
                testTypes = values.groupBy { it.testType }.keys 
            )
        }
        sender.send(
            agentInfo,
            "/active-sessions",
            ActiveSessions.serializer() stringify activeSessions
        )
    }

    internal suspend fun sendActiveScope() {
        sender.send(
            agentInfo,
            "/active-scope",
            ScopeSummary.serializer() stringify agentState.activeScope.summary
        )
    }

    internal suspend fun sendScopes() {
        sender.send(
            agentInfo,
            "/scopes",
            ScopeSummary.serializer().list stringify agentState.scopeSummaries
        )
    }

    internal suspend fun toggleScope(scopeId: String) {
        agentState.scopes[scopeId]?.let { scope ->
            scope.toggle()
            sendScopes()
            //todo send build coverage
        }
    }

    internal suspend fun dropScope(scopeId: String) {
        agentState.scopes.remove(scopeId)?.let {
            sendScopes()
            //todo send build coverage
        }
    }

    internal suspend fun changeActiveScope(scopeChange: ActiveScopeChangePayload) {
        val prevScope = agentState.changeActiveScope(scopeChange.scopeName)
        if (scopeChange.savePrevScope) {
            if (prevScope.any()) {
                val finishedScope = prevScope.finish()
                println("Scope \"${finishedScope.name}\" have been saved with id \"${finishedScope.id}\"")
                agentState.scopes[finishedScope.id] = finishedScope
                //todo send finished scope coverage
                //todo send build coverage
            } else {
                println("Scope \"${prevScope.name}\" is empty, it won't be added to the build.")
            }
        }
        println("Current active scope name is \"${agentState.activeScope.name}\"")
        sendScopeMessages()
    }

    internal suspend fun sendCalcResults(cis: CoverageInfoSet, path: String = "") {
        // TODO extend destination with plugin id
        if (cis.associatedTests.isNotEmpty()) {
            println("Assoc tests - ids count: ${cis.associatedTests.count()}")
            sender.send(
                agentInfo,
                "$path/associated-tests",
                AssociatedTests.serializer().list stringify cis.associatedTests
            )
        }
        sendCoverageBlock(cis.coverageBlock, path)
        sender.send(
            agentInfo,
            "$path/coverage-new",
            NewCoverageBlock.serializer() stringify cis.newCoverageBlock
        )
        sender.send(
            agentInfo,
            "$path/new-methods",
            SimpleJavaMethodCoverage.serializer().list stringify cis.newMethodsCoverages
        )
        val packageCoverage = cis.packageCoverage
        sendPackageCoverage(packageCoverage, path)
        sender.send(
            agentInfo,
            "$path/tests-usages",
            TestUsagesInfo.serializer().list stringify cis.testUsages
        )
    }

    internal suspend fun sendPackageCoverage(
        packageCoverage: List<JavaPackageCoverage>,
        path: String = ""
    ) {
        sender.send(
            agentInfo,
            "$path/coverage-by-packages",
            JavaPackageCoverage.serializer().list stringify packageCoverage
        )
    }

    internal suspend fun sendCoverageBlock(
        coverageBlock: CoverageBlock,
        path: String = ""
    ) {
        sender.send(
            agentInfo,
            "$path/coverage",
            CoverageBlock.serializer() stringify coverageBlock
        )
    }

}
