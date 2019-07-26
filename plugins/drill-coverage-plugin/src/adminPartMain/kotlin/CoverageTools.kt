package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.*

//TODO Rewrite all of this, remove the file

data class CoverageInfoSet(
    val associatedTests: List<AssociatedTests>,
    val coverageBlock: CoverageBlock,
    val coverageByType: Map<String, TestTypeSummary>,
    val changedCoverageBlock: ChangedCoverageBlock,
    val changedCoverages: ChangedCoverages,
    val packageCoverage: List<JavaPackageCoverage>,
    val testUsages: List<TestUsagesInfo>
)

fun testUsages(bundleMap: Map<TypedTest, IBundleCoverage>): List<TestUsagesInfo> =
    bundleMap.map { (test, bundle) ->
        TestUsagesInfo(test.name, bundle.methodCounter.coveredCount, test.type)
    }

fun packageCoverage(
    bundleCoverage: IBundleCoverage,
    assocTestsMap: Map<CoverageKey, List<TypedTest>>
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
    assocTestsMap: Map<CoverageKey, List<TypedTest>>
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

fun Map<CoverageKey, List<TypedTest>>.getAssociatedTests() = map { (key, tests) ->
    AssociatedTests(
        id = key.id,
        packageName = key.packageName,
        className = key.className,
        methodName = key.methodName,
        tests = tests.map { it.toString() }
    )
}

fun calculateNewCoverageBlock(
    methodChanges: MethodChanges,
    bundleCoverage: IBundleCoverage
) = if (methodChanges.methodsChanged) {
    val methodsCoverages = bundleCoverage.packages
        .flatMap { it.classes }
        .flatMap { c -> c.methods.map { (c.name to it.sign()) to it } }.toMap()

    val (newInstructionCounters, newCoverages) =
            methodChanges.new.getInfo(methodsCoverages)
    val (newCoverage, newCounter) = newInstructionCounters.calculateCoverage()

    val (modifiedInstructionCounters, modifiedCoverages) =
            methodChanges.modified.getInfo(methodsCoverages)
    val (modifiedCoverage, modifiedCounter) = modifiedInstructionCounters.calculateCoverage()

    ChangedCoverageBlock(
        newCoverages.count(),
        newCounter,
        modifiedCoverages.count(),
        modifiedCounter,
        methodChanges.deleted.count(),
        newCoverage,
        modifiedCoverage
    ) to
            ChangedCoverages(
                newCoverages,
                modifiedCoverages,
                methodChanges.deleted
            )
} else {
    ChangedCoverageBlock() to ChangedCoverages()
}


fun List<JavaMethod>.getInfo(
    data: Map<Pair<String, String>, IMethodCoverage>
) = mapNotNull {
    data[it.ownerClass to it.sign]?.run {
        (instructionCounter.totalCount to instructionCounter.coveredCount) to simpleMethodCoverage(it.ownerClass)
    }
}.run { map { it.first } to map { it.second } }

fun List<Pair<Int, Int>>.calculateCoverage() = if (isNotEmpty()) {
    val total = map { it.first }.reduce { acc, el -> acc + el }
    val covered = map { it.second }.reduce { acc, el -> acc + el }
    val coveredCount = map { it.second }.count { it > 0 }
    val coverage = if (total > 0) covered.toDouble() / total * 100 else 0.0
    coverage to coveredCount
} else 0.0 to 0

fun IMethodCoverage.sign() = "$name$desc"
