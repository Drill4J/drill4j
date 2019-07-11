package com.epam.drill.plugins.coverage

import kotlin.test.*

class SerdeTest {

    @Test
    fun `serde action StartNewSession`() {
        val action = StartNewSession(payload = StartPayload(testType = "MANUAL"))
        val str = commonSerDe.stringify(commonSerDe.actionSerializer, action)
        val parsedAction = commonSerDe.parse(commonSerDe.actionSerializer, str)
        assertEquals(action, parsedAction)
        
    }
}