package com.epam.drill.plugins.coverage


import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.SerDe
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.Sender
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import kotlinx.serialization.set
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.data.ExecutionDataStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal val agentStates = ConcurrentHashMap<String, AgentState>()

//TODO This is a temporary storage API. It will be removed when the core API has been developed
internal val agentStorages = ConcurrentHashMap<String, Storage>()

@Suppress("unused", "MemberVisibilityCanBePrivate")
class CoverageController(sender: Sender, agentInfo: AgentInfo, id: String) :
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

    override suspend fun doAction(action: Action): Any {
        return when (action) {
            is SwitchScope -> checkoutScope(action.payload.scopeName)
            is IgnoreScope -> toggleScope(action.payload.scopeName, action.payload.enabled)
            is DropScope -> dropScope(action.payload.scopeName)
            is StartSession -> SessionPayload.serializer() stringify SessionPayload(UUID.randomUUID().toString())
            else -> Unit
        }
    }

    override suspend fun processData(dm: DrillMessage): Any {
        val content = dm.content
        val message = CoverageMessage.serializer() parse content!!
        return processData(message)
    }

    internal suspend fun processData(parse: CoverageMessage): Any {
        when (parse.type) {
            CoverageEventType.INIT -> {
                updateScopeMessages()
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
                    val defaultScope = Scope()
                    storage.store(ScopeKey(agentInfo.buildVersion), defaultScope)
                    processData(CoverageMessage(CoverageEventType.SESSION_FINISHED, ""))
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
                val scope = storage.retrieve(scopeKey)!!
                scope.incSessionCount()
                val classesData = agentState.classesData()
                scope.probes.addAll(classesData.execData.stop())
                val cis = calculateCoverageData(scope.probes)
                updateGatheringState(false)
                sendCalcResults(cis)
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

    internal suspend fun updateScopeMessages() {
        val key = ScopesKey(agentInfo.buildVersion)
        val scopes = storage.retrieve(key) ?: mutableSetOf()
        storage.store(key, scopes + scopeKey.name)
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

    internal suspend fun sendScopes(scopes: Set<String>) {
        sender.send(
            agentInfo,
            "/scopes",
            String.serializer().set stringify scopes
        )
    }

    internal suspend fun toggleScope(scopeName: String, enabled: Boolean) {
        val toggleScopeKey = ScopeKey(agentInfo.buildVersion, scopeName)
        val scope = storage.retrieve(toggleScopeKey)
        scope?.enabled = enabled
    }

    internal suspend fun dropScope(scopeName: String) {
        val dropScopeFromSetKey = ScopesKey(agentInfo.buildVersion)
        val currentScopeSet = storage.retrieve(dropScopeFromSetKey) ?: setOf()
        if (scopeName in currentScopeSet) {
            val processedScopeSet = currentScopeSet - scopeName
            storage.store(dropScopeFromSetKey, processedScopeSet)
            sendScopes(processedScopeSet)
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
            updateScopeMessages()
            val scope = storage.retrieve(scopeKey) ?: Scope().apply { storage.store(scopeKey, this) }

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