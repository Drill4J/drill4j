package com.epam.drill.plugins.coverage

import kotlin.test.*

class DataExtTest {

    @Test
    fun `ByteArray-encode works correctly`() {
        val testArray = "some string".toByteArray()
        val encoded = testArray.encode()
        assertEquals("c29tZSBzdHJpbmc=", encoded)
    }

    @Test
    fun `EncodedString-decode works correctly`() {
        val encodedString: EncodedString = "c29tZSBzdHJpbmc="
        val actual = encodedString.decode()
        val expected = "some string".toByteArray()
        assertTrue(expected contentEquals actual, "Expected <$expected>, actual <$actual>")
    }


    @Test
    fun `Encodes-Decodes correctly`() {
        val expected = "some string".toByteArray()
        val encodedArray = expected.encode()
        val actual = encodedArray.decode()
        assertTrue(expected contentEquals actual, "Expected <$expected>, actual <$actual>")
    }
}

