package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.IMethodCoverage
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataStore

fun testUsages(bundleMap: Map<String, IBundleCoverage>): List<TestUsagesInfo> =
    bundleMap.map { (k, v) ->
        TestUsagesInfo(k, v.methodCounter.coveredCount, "Test type", "30.02.2019")
    }

fun testUsageBundles(
    initialClassBytes: Map<String, ByteArray>,
    probes: Collection<ExDataTemp>
): Map<String, IBundleCoverage> = probes
    .filter { it.testName != null }
    .groupBy { it.testName!! }
    .mapValues { (_, v) ->
        val dataStore = ExecutionDataStore()
        v.forEach {
            val probeArray = it.probes.toBooleanArray()
            val executionData = ExecutionData(it.id, it.className, probeArray)
            dataStore.put(executionData)
        }
        testUsageBundle(initialClassBytes, dataStore)
    }

fun testUsageBundle(
    initialClassBytes: Map<String, ByteArray>,
    dataStore: ExecutionDataStore
): IBundleCoverage {
    val coverageBuilder = CoverageBuilder()
    val analyzer = Analyzer(dataStore, coverageBuilder)
    dataStore.contents.forEach { execData ->
        analyzer.analyzeClass(initialClassBytes[execData.name], execData.name)
    }
    return coverageBuilder.getBundle("all")
}

fun collectAssocTestPairs(
    initialClassBytes: Map<String, ByteArray>,
    executionData: ExecutionData,
    testName: String
): List<Pair<CoverageKey, String>> {
    val cb = CoverageBuilder()
    Analyzer(ExecutionDataStore().apply { put(executionData) }, cb).analyzeClass(
        initialClassBytes[executionData.name],
        executionData.name
    )
    return cb.getBundle("").packages.flatMap { p ->
        listOf(p.coverageKey() to testName) + p.classes.flatMap { c ->
            listOf(c.coverageKey() to testName) + c.methods.flatMap { m ->
                if (m.instructionCounter.coveredCount > 0) {
                    listOf(m.coverageKey(c) to testName)
                } else emptyList()
            }
        }
    }
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

fun methodCoverageId(
    classCoverage: IClassCoverage,
    methodCoverage: IMethodCoverage
) = "${classCoverage.name}.${methodCoverage.name}${methodCoverage.desc}".crc64