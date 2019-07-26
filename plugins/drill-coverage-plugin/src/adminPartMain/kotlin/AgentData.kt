package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import io.vavr.kotlin.*
import kotlinx.atomicfu.*
import org.jacoco.core.analysis.*

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
    val totals: ICoverageNode,
    val javaMethods: Map<String, Methods>,
    val prevAgentInfo: AgentInfo?,
    val methodsChanges: MethodChanges,
    val prevBuildCoverage: Double,
    val changed: Boolean
) : AgentData() {

    private val _lastBuildCoverage = atomic(0.0)

    var lastBuildCoverage
        get() = _lastBuildCoverage.value
        set(value) {
            _lastBuildCoverage.value = value
        }
}

typealias MethodChanges = Map<DiffType, Methods>

fun MethodChanges.notEmpty() = keys
    .filter { it != DiffType.UNAFFECTED }
    .mapNotNull { this[it] }
    .flatten()
    .isNotEmpty()
