package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import io.vavr.kotlin.*
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
        agentData.addClass(key, bytes)
    }

    fun initialized() {
        //throw ClassCastException if the ref value is in the wrong state
        val agentData = data as ClassDataBuilder
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(ExecutionDataStore(), coverageBuilder)
        val classBytes = LinkedHashMap<String, ByteArray>(agentData.count)
        for (pair in agentData.classData) {
            classBytes[pair.first] = pair.second
            analyzer.analyzeClass(pair.second, pair.first)
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


    private val _classData = atomic(list<Pair<String, ByteArray>>())
    
    val classData get() = _classData.value

    fun addClass(name: String, body: ByteArray) {
        _classData.update { it.append(name to body) }
    }
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

    private val _data = atomic(list<ExDataTemp>())

    @Volatile
    var coverage: Double? = null

    fun start() {
        _data.value = list()
    }

    fun add(probe: ExDataTemp) {
        _data.update { it.append(probe) }
    }

    fun stop() = _data.getAndUpdate { list() }
}
