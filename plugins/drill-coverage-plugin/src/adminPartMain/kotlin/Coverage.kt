package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.*
import org.jacoco.core.data.*

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
