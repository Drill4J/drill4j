package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.agentmanager.hasValidParameters
import com.epam.drill.common.AgentStatus
import org.junit.Test
import kotlin.test.assertEquals

class RegexTest {
    @Test
    fun regTest() {
        val testSubject = AgentInfoWebSocketSingle(
            id = "test",
            name = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            buildVersion = "testVersion",
            description = "testD--  #...esc",
            status = AgentStatus.NOT_REGISTERED,
            buildAlias = "testAlias"
        )
        assertEquals(true, testSubject.hasValidParameters())
    }

}