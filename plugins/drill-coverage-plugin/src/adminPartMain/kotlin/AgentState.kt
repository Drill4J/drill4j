package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import kotlinx.atomicfu.*
import org.jacoco.core.analysis.*
import org.jacoco.core.data.*
import org.javers.core.*
import org.javers.core.diff.changetype.*

/**
 * Agent state.
 * The state itself holds only an atomic reference to the data.
 * The data is represented by the sealed class hierarchy AgentData.
 * In case of inconsistencies of the data a ClassCastException is thrown.
 */
class AgentState(
    val agentInfo: AgentInfo,
    prevState: AgentState?
) {
    @Suppress("PropertyName")
    private val _data = atomic(prevState?.data ?: NoData)

    internal var data: AgentData
        get() = _data.value
        private set(value) {
            _data.value = value
        }

    private val javers = JaversBuilder.javers().build()

    private val _activeScope = atomic(ActiveScope(""))

    val activeScope get() = _activeScope.value

    val activeSessions = AtomicCache<String, ActiveSession>()

    val scopes = AtomicCache<String, FinishedScope>()

    val scopeSummaries get() = scopes.values.map { it.summary } 

    fun init(initInfo: InitInfo) {
        _data.updateAndGet { prevData ->
            ClassDataBuilder(
                count = initInfo.classesCount,
                prevData = prevData as? ClassesData
            )
        }
    }

    fun addClass(key: String, bytes: ByteArray) {
        //throw ClassCastException if the ref value is in the wrong state
        val agentData = data as ClassDataBuilder
        agentData.addClass(key, bytes)
    }

    fun initialized() {
        //throw ClassCastException if the ref value is in the wrong state
        val agentData = data as ClassDataBuilder
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(ExecutionDataStore(), coverageBuilder)
        agentData.classData.toMap()
        val classBytes = agentData.classData.asSequence()
            .onEach { analyzer.analyzeClass(it.second, it.first) }
            .toMap()
        val bundleCoverage = coverageBuilder.getBundle("")
        val javaClasses = bundleCoverage.javaClasses
        val prevData = agentData.prevData
        val prevClassesSet = prevData?.javaClasses?.values?.toSet().orEmpty()
        val currClassesSet = javaClasses.values.toSet()
        val diff = javers.compareCollections(
            prevClassesSet,
            currClassesSet,
            JavaClass::class.java
        )
        val diffNewMethods = diff.getObjectsByChangeType(NewObject::class.java).filterIsInstance<JavaMethod>()
        val prevAgentInfo = prevData?.agentInfo
        val (newMethods, changed) = when {
            agentInfo == prevAgentInfo && diffNewMethods.isEmpty() -> prevData.newMethods to false
            else -> diffNewMethods to true
        }
        data = ClassesData(
            agentInfo = agentInfo,
            classesBytes = classBytes,
            totals = bundleCoverage.plainCopy,
            totalsMap = bundleCoverage.totalsMap,
            javaClasses = javaClasses,
            newMethods = newMethods,
            prevBuildCoverage = prevData?.lastBuildCoverage ?: 0.0,
            changed = changed
        )
    }

    //throw ClassCastException if the ref value is in the wrong state
    fun classesData(): ClassesData = data as ClassesData

    fun changeActiveScope(name: String) = _activeScope.getAndUpdate { ActiveScope(name) }

    fun startSession(msg: SessionStarted) {
        activeSessions(msg.sessionId) { ActiveSession(msg.sessionId, msg.testType) }
    }

    fun addProbes(msg: CoverDataPart) {
        activeSessions[msg.sessionId]?.let { activeSession ->
            for (probe in msg.data) {
                activeSession.append(probe)
            }
        }
    }

    fun cancelSession(msg: SessionCancelled) = activeSessions.remove(msg.sessionId)

    fun finishSession(msg: SessionFinished): FinishedSession? {
        return when (val activeSession = activeSessions.remove(msg.sessionId)) {
            null -> null
            else -> activeSession.finish()
        }
    }
}
