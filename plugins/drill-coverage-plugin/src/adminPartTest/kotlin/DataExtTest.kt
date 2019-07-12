package com.epam.drill.plugins.coverage

import java.util.*
import kotlin.test.*

class DataExtAdminTest {

    @Test
    fun `Base64-encoded string decodes correct`() {
        val testArray = "some string".toByteArray()
        val encodedArray = Base64.getEncoder().encodeToString(testArray) as EncodedString
        val decodedArray = encodedArray.decode()
        assertTrue(testArray contentEquals decodedArray)
    }
}