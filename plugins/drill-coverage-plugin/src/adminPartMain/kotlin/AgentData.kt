package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import io.vavr.kotlin.*
import kotlinx.atomicfu.*

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
        val classesCount: Int,
        val methodsCount: Int,
        val instructionsCount: Int,
        val javaClasses: Map<String, JavaClass>,
        val newMethods: List<JavaMethod>,
        val changed: Boolean
) : AgentData()