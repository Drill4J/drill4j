package com.epam.drill

import com.epam.drill.core.ws.parseRawPluginFromAdminStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WsActionLoadTest {

    @Test
    fun `should return plugin name and some data`() {
//        @formatter:off
        val testData =
            byteArrayOf(99, 117, 115, 116, 111, 109, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 80, 75, 3)
//        @formatter:on
        val (name, someData) = parseRawPluginFromAdminStorage(testData)
        assertEquals("custom", name)
        assertNotNull(someData)

    }


}