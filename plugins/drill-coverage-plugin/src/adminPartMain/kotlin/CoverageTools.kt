package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.*

//TODO Rewrite all of this, remove the file

data class CoverageInfoSet(
    val associatedTests: List<AssociatedTests>,
    val coverage: Coverage,
    val buildMethods: BuildMethods,
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

fun IBundleCoverage.toDataMap() = packages
    .flatMap { it.classes }
    .flatMap { c -> c.methods.map { (c.name to it.sign()) to it } }.toMap()

fun calculateBuildMethods(
    methodChanges: MethodChanges,
    bundleCoverage: IBundleCoverage
): BuildMethods {
    val methodsCoverages = bundleCoverage.toDataMap()

    val infos = DiffType.values().map { type ->
        type to (methodChanges[type]?.getInfo(methodsCoverages) ?: MethodsInfo())
    }.toMap()

    val totalInfo = infos
        .keys.filter { it != DiffType.DELETED }
        .mapNotNull { infos[it] }
        .reduce { totalInfo, info ->
            MethodsInfo(
                totalInfo.totalCount + info.totalCount,
                totalInfo.coveredCount + info.coveredCount,
                totalInfo.methods + info.methods
            )
        }

    return BuildMethods(
        totalMethods = totalInfo,
        newMethods = infos[DiffType.NEW]!!,
        modifiedNameMethods = infos[DiffType.MODIFIED_NAME]!!,
        modifiedDescMethods = infos[DiffType.MODIFIED_DESC]!!,
        modifiedBodyMethods = infos[DiffType.MODIFIED_BODY]!!,
        deletedMethods = infos[DiffType.DELETED]!!
    )
}


fun Methods.getInfo(
    data: Map<Pair<String, String>, IMethodCoverage>
) = MethodsInfo(
    totalCount = this.count(),
    coveredCount = count { data[it.ownerClass to it.sign]?.instructionCounter?.coveredCount ?: 0 > 0 },
    methods = this.map { method ->
        val rate = data[method.ownerClass to method.sign]?.coverageRate() ?: CoverageRate.MISSED
        method.copy(coverageRate = rate)
    }
)

fun IMethodCoverage.sign() = "$name$desc"
