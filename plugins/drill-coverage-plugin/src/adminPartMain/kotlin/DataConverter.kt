import com.epam.drill.plugins.coverage.dataclasses.RawClassData
import com.epam.drill.plugins.coverage.dataclasses.RawScope
import com.epam.drill.plugins.coverage.dataclasses.RawTest

fun convert(rawScope: RawScope): Scope {
    return Scope(
        rawScope.id,
        rawScope.name,
        rawScope.buildVersion,
        rawScope.tests.map { rawTest ->
            convert(rawTest)
        }
    )
}

fun convert(rawTest: RawTest): Test {
    return Test(
        rawTest.id,
        rawTest.testName,
        rawTest.testType,
        rawTest.data.map { rawClassData ->
            convert(rawClassData)
        }
    )
}

fun convert(rawClassData: RawClassData): ClassData {
    return ClassData(
        rawClassData.id,
        rawClassData.className,
        rawClassData.probes
    )
}

