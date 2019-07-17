package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.*
import org.jacoco.core.data.*
import kotlin.math.*

fun ClassesData.coverageBundle(data: Sequence<ExecClassData>): IBundleCoverage {
    val coverageBuilder = CoverageBuilder()
    val dataStore = ExecutionDataStore().with(data)
    val analyzer = Analyzer(dataStore, coverageBuilder)
    dataStore.contents.forEach {
        analyzer.analyzeClass(classesBytes[it.name], it.name)
    }
    return coverageBuilder.getBundle("")
}

fun ClassesData.coverage(data: Sequence<ExecClassData>) =
    coverageBundle(data).coverage(totals.instructionCounter.totalCount)

fun ClassesData.arrowType(totalCoveragePercent: Double): ArrowType? {
    val diff = totalCoveragePercent - prevBuildCoverage
    return when {
        abs(diff) < 1E-7 -> null
        diff > 0.0 -> ArrowType.INCREASE
        else -> ArrowType.DECREASE
    }
}