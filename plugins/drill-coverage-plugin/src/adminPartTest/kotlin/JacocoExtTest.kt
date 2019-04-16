package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.CoverageNodeImpl
import org.jacoco.core.analysis.ICoverageNode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class JacocoExtTest {

    @Test
    fun `crcr64 should return same values for equal strings`() {
        val str1 = javaClass.name.substringAfterLast(".")
        val str2 = javaClass.simpleName
        assertNotSame(str1, str2) //the strings should not be identical
        assertEquals(str1, str2)
        assertEquals(str1.crc64, str2.crc64)
    }

    @Test
    fun `coverage should return null for non-finite ratiios`() {
        val coverageNode = CoverageNodeImpl(ICoverageNode.ElementType.METHOD, "test")
        val counter = coverageNode.methodCounter
        assertEquals(0, counter.totalCount)
        assertFalse { counter.coveredRatio.isFinite() }
        assertNull(coverageNode.coverage)
    }
}