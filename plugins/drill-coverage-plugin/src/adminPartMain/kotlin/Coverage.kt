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

fun ClassesData.coverage(data: Sequence<FinishedSession>) =
    coverageBundle(data.flatten()).coverage(totals.instructionCounter.totalCount)

fun ClassesData.coveragesByTestType(data: Sequence<FinishedSession>): Map<String, TestTypeSummary> {
    return data.groupBy { it.testType }.mapValues { (testType, finishedSessions) ->
        TestTypeSummary(
            testType = testType,
            coverage = coverage(finishedSessions.asSequence()),
            testCount = finishedSessions.flatMap { it.testNames }.distinct().count()
        )
    }
}

fun ClassesData.arrowType(totalCoveragePercent: Double): ArrowType? {
    val diff = totalCoveragePercent - prevBuildCoverage
    return when {
        abs(diff) < 1E-7 -> null
        diff > 0.0 -> ArrowType.INCREASE
        else -> ArrowType.DECREASE
    }
}

fun ClassesData.associatedTests(data: Sequence<FinishedSession>) = bundlesByTests(data)
    .flatMap { (testName, bundle) ->
        bundle.collectAssocTestPairs(testName)
    }.groupBy({ it.first }) { it.second } //group by test names
    .mapValues { it.value.distinct() }

fun ClassesData.bundlesByTests(data: Sequence<FinishedSession>): Map<String, IBundleCoverage> {
    return data.flatMap { it.probes.asSequence() }
        .groupBy({ it.key }) { it.value }
        .mapValues { it.value.asSequence().flatten() }
        .mapValues { coverageBundle(it.value) }
}

fun testUsages(bundleMap: Map<String, IBundleCoverage>): List<TestUsagesInfo> =
    bundleMap.map { (k, v) ->
        //TODO: reconsider the solution, rewrite this method
        val (name, type) = k.split("::").let { it[1] to it[0] }
        TestUsagesInfo(name, v.methodCounter.coveredCount, type, "30.02.2019 (stub)")
    }

fun packageCoverage(
    bundleCoverage: IBundleCoverage,
    assocTestsMap: Map<CoverageKey, List<String>>
): List<JavaPackageCoverage> = bundleCoverage.packages
    .map { packageCoverage ->
        val packageKey = packageCoverage.coverageKey()
        JavaPackageCoverage(
            id = packageKey.id,
            name = packageCoverage.name,
            coverage = packageCoverage.coverage,
            totalClassesCount = packageCoverage.classCounter.totalCount,
            coveredClassesCount = packageCoverage.classCounter.coveredCount,
            totalMethodsCount = packageCoverage.methodCounter.totalCount,
            coveredMethodsCount = packageCoverage.methodCounter.coveredCount,
            assocTestsCount = assocTestsMap[packageKey]?.count(),
            classes = classCoverage(packageCoverage.classes, assocTestsMap)
        )
    }.toList()

fun classCoverage(
    classCoverages: Collection<IClassCoverage>,
    assocTestsMap: Map<CoverageKey, List<String>>
): List<JavaClassCoverage> = classCoverages
    .map { classCoverage ->
        val classKey = classCoverage.coverageKey()
        JavaClassCoverage(
            id = classKey.id,
            name = classCoverage.name.substringAfterLast('/'),
            path = classCoverage.name,
            coverage = classCoverage.coverage,
            totalMethodsCount = classCoverage.methodCounter.totalCount,
            coveredMethodsCount = classCoverage.methodCounter.coveredCount,
            assocTestsCount = assocTestsMap[classKey]?.count(),
            methods = classCoverage.methods.map { methodCoverage ->
                val methodKey = methodCoverage.coverageKey(classCoverage)
                JavaMethodCoverage(
                    id = methodKey.id,
                    name = methodCoverage.name,
                    desc = methodCoverage.desc,
                    decl = declaration(methodCoverage.desc),
                    coverage = methodCoverage.coverage,
                    assocTestsCount = assocTestsMap[methodKey]?.count()
                )
            }.toList()
        )
    }.toList()

fun Map<CoverageKey, List<String>>.getAssociatedTests() = map { (key, tests) ->
    AssociatedTests(
        id = key.id,
        packageName = key.packageName,
        className = key.className,
        methodName = key.methodName,
        tests = tests
    )
}

fun calculateNewCoverageBlock(
    newMethods: List<JavaMethod>,
    bundleCoverage: IBundleCoverage
) = if (newMethods.isNotEmpty()) {
    val newMethodsCoverages = bundleCoverage.packages
        .flatMap { it.classes }
        .flatMap { clazz -> clazz.methods
                .map { method -> Pair(JavaMethod(clazz.name, method.name, method.desc), method) } }
        .filter { it.first in newMethods }
    val newCoverage = methodsCoverageByInstructions(newMethodsCoverages.map { it.second })
    NewCoverageBlock(
        methodsCount = newMethodsCoverages.count(),
        methodsCovered = newMethodsCoverages.count { it.second.methodCounter.coveredCount > 0 },
        coverage = newCoverage
    ) to newMethodsCoverages.map { (jm, mc) -> mc.simpleMethodCoverage(jm.ownerClass) }
} else NewCoverageBlock() to emptyList()

fun methodsCoverageByInstructions(methodsCoverages: List<IMethodCoverage>) =
    when (val totalCount = methodsCoverages.sumBy { it.instructionCounter.totalCount }) {
        0 -> 0.0
        else -> methodsCoverages.sumBy { it.instructionCounter.coveredCount }.toDouble() / totalCount * 100
    }