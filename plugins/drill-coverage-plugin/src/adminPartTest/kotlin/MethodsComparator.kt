package com.epam.drill.plugins.coverage.test

import com.epam.drill.plugins.coverage.*
import java.io.*
import java.nio.file.*
import kotlin.test.*

class MethodsComparatorTest {

    private val stubsDir = "${Paths.get(".").toAbsolutePath().normalize()}/build/processedResources/adminPart/test/"
    private val origin = getMethods("${stubsDir}Rst1.class")
    private val oneAdded = getMethods("${stubsDir}Rst2.class")
    private val oneDeletedOneModified = getMethods("${stubsDir}Rst3.class")

    @Test
    fun `should detect 3 new methods`() {
        val res = MethodsComparator().compareClasses(emptyMap(), origin)
        assertTrue { res[DiffType.UNAFFECTED].isNullOrEmpty() }
        assertTrue { res[DiffType.DELETED].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_NAME].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_DESC].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_BODY].isNullOrEmpty() }
        assertTrue { res[DiffType.NEW]!!.size == 3 }
    }

    @Test
    fun `should detect one new method`() {
        val res = MethodsComparator().compareClasses(origin, oneAdded)
        assertTrue { res[DiffType.UNAFFECTED]!!.size == 3 }
        assertTrue { res[DiffType.DELETED].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_NAME].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_DESC].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_BODY].isNullOrEmpty() }
        assertTrue { res[DiffType.NEW]!!.size == 1 }
    }

    @Test
    fun `should detect 1 modified by body and 1 deleted methods`() {
        val res = MethodsComparator().compareClasses(origin, oneDeletedOneModified)
        assertTrue { res[DiffType.UNAFFECTED]!!.size == 1 }
        assertTrue { res[DiffType.DELETED]!!.size == 1 }
        assertTrue { res[DiffType.MODIFIED_NAME].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_DESC].isNullOrEmpty() }
        assertTrue { res[DiffType.MODIFIED_BODY]!!.size == 1 }
        assertTrue { res[DiffType.NEW].isNullOrEmpty() }
    }

    fun getMethods(path: String) = listOf(
        "abc.Rst" to ASMClassParser(
            File(path).inputStream().readBytes(),
            "abc.Rst"
        ).parseToJavaMethods()
    ).toMap()
}
