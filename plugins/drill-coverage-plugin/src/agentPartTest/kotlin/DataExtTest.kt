package com.epam.drill.plugins.coverage

import java.util.*
import kotlin.test.*

class DataExtAgentTest {

    @Test
    fun `String Base64-encodes correct`() {
        val testArray = "some string".toByteArray()
        val encodedArray = testArray.encode()
        val decodedArray = Base64.getDecoder().decode(encodedArray)
        assertTrue(testArray contentEquals decodedArray)
    }
}