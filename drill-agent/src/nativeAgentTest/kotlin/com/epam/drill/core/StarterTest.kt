package com.epam.drill.core

import kotlin.test.*

class StarterTest {
    @Test
    fun `Agent params parser should return an empty map when apply an empty or null string`() {
        assertEquals(mutableMapOf(), "".asAgentParams())
        val nullString: String? = null
        assertEquals(mutableMapOf(), nullString.asAgentParams())
    }

    @Test
    fun `Agent params parser should return two values map when apply xx=123,sad=jjk`() {

        assertEquals(2, ARGUMENT_TEST_DATA.asAgentParams().size)
    }

    @Test
    fun `Agent params parser should return equalityContents map when apply xx=123,sad=jjk`() {
        assertEquals(
            mutableMapOf(
                "xx" to "123", "sad" to "jjk"
            ), ARGUMENT_TEST_DATA.asAgentParams()
        )
    }

    @Test
    fun `Agent params parser should thrown an @IllegalArgumentException when apply the wrong string args`() {
        assertFailsWith(IllegalArgumentException::class) { "uncontrolledParameter with spaces".asAgentParams() }
    }

    companion object {
        const val ARGUMENT_TEST_DATA = "xx=123,sad=jjk"
    }
}


