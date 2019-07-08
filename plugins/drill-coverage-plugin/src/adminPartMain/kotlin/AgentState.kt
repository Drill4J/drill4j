package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import kotlinx.atomicfu.*
import org.jacoco.core.analysis.*
import org.jacoco.core.data.*
import org.javers.core.*
import org.javers.core.diff.changetype.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

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
    internal val _data = atomic(prevState?.data ?: NoData)

    internal var data: AgentData
        get() = _data.value
        private set(value) {
            _data.value = value
        }

    private val javers = JaversBuilder.javers().build()

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
        agentData.classData.offer(key to bytes)
    }

    fun initialized() {
        //throw ClassCastException if the ref value is in the wrong state
        val agentData = data as ClassDataBuilder
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(ExecutionDataStore(), coverageBuilder)
        val classBytes = LinkedHashMap<String, ByteArray>(agentData.count)
        while (true) {
            val pair = agentData.classData.poll()
            if (pair != null) {
                classBytes[pair.first] = pair.second
                analyzer.analyzeClass(pair.second, pair.first)
            } else break
        }
        val bundleCoverage = coverageBuilder.getBundle("")
        val javaClasses = bundleCoverage.packages
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
            }.toMap()
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
            javaClasses = javaClasses,
            newMethods = newMethods,
            changed = changed
        )
    }

    //throw ClassCastException if the ref value is in the wrong state
    fun classesData(): ClassesData = data as ClassesData
}

sealed class AgentData

object NoData : AgentData()

class ClassDataBuilder(
    val count: Int,
    val prevData: ClassesData?
) : AgentData() {
    internal val classData = ConcurrentLinkedQueue<Pair<String, ByteArray>>()
}

class ClassesData(
    val agentInfo: AgentInfo,
    val classesBytes: Map<String, ByteArray>,
    val javaClasses: Map<String, JavaClass>,
    val newMethods: List<JavaMethod>,
    val changed: Boolean
) : AgentData() {
    val execData = ExecData()
}

class ExecData {

    private val dataRef = AtomicReference<MutableCollection<ExDataTemp>>()

    @Volatile
    var coverage: Double? = null

    fun start() = dataRef.set(ConcurrentLinkedQueue())

    fun add(probe: ExDataTemp) {
        dataRef.get()!!.add(probe)
    }

    fun stop() = dataRef.getAndSet(null) as Collection<ExDataTemp>? ?: emptyList()
}

