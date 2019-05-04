package com.epam.drill.plugins.coverage

import com.epam.drill.common.AgentInfo
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.data.ExecutionDataStore
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.NewObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Agent state.
 * The state itself holds only an atomic reference to the data.
 * The data is represented by the sealed class hierarchy AgentData.
 * In case of inconsistencies of the data a ClassCastException is thrown.
 */
class AgentState(
    val agentInfo: AgentInfo
) {
    internal val dataRef = AtomicReference<AgentData>(NoData)

    private val javers = JaversBuilder.javers().build()

    fun init(initInfo: InitInfo) {
        dataRef.updateAndGet { prevData ->
            ClassDataBuilder(
                count = initInfo.classesCount,
                prevJavaClasses = when (prevData) {
                    is ClassesData -> prevData.javaClasses
                    else -> emptyMap()
                }
            )
        }
    }

    fun addClass(key: String, bytes: ByteArray) {
        //throw ClassCastException if the ref value is in the wrong state
        val agentData = dataRef.get() as ClassDataBuilder
        agentData.chan.offer(key to bytes)
    }

    fun initialized() {
        //throw ClassCastException if the ref value is in the wrong state
        val agentData = dataRef.get() as ClassDataBuilder
        val coverageBuilder = CoverageBuilder()
        val analyzer = Analyzer(ExecutionDataStore(), coverageBuilder)
        val classBytes = LinkedHashMap<String, ByteArray>(agentData.count)
        while (true) {
            val pair = agentData.chan.poll()
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
        val diff = javers.compareCollections(
            agentData.prevJavaClasses.values.toList(),
            javaClasses.values.toList(),
            JavaClass::class.java
        )
        val newMethods = diff.getObjectsByChangeType(NewObject::class.java).filterIsInstance<JavaMethod>()

        dataRef.set(
            ClassesData(
                classesBytes = classBytes,
                javaClasses = javaClasses,
                newMethods = newMethods
            )
        )
    }

    //throw ClassCastException if the ref value is in the wrong state
    fun classesData(): ClassesData = dataRef.get() as ClassesData
}

sealed class AgentData

object NoData : AgentData()

class ClassDataBuilder(
    val count: Int,
    val prevJavaClasses: Map<String, JavaClass>
) : AgentData() {
    internal val chan = ConcurrentLinkedQueue<Pair<String, ByteArray>>()
}

class ClassesData(
    val classesBytes: Map<String, ByteArray>,
    val javaClasses: Map<String, JavaClass>,
    val newMethods: List<JavaMethod>
) : AgentData()

