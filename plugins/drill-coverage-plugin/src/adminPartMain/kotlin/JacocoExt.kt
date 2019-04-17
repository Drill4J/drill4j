package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.core.internal.data.CRC64

val String.crc64: String get() = CRC64.classId(toByteArray()).toString(Character.MAX_RADIX)

val ICoverageNode.coverage: Double?
    get() {
        val ratio = this.instructionCounter.coveredRatio
        return if (ratio.isFinite()) ratio * 100.0 else null
    }
