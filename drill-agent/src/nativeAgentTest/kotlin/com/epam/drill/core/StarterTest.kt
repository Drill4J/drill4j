package com.epam.drill.core

import drillInternal.addMessage
import drillInternal.createQueue
import drillInternal.getMessage
import drillInternal.messageQu
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    /**all features is provided by drill-agent/src/nativeInterop/cinterop/drillInternal.def file.*/
    @Test
    fun `Queue for ws messages should create a Clang dynamyc arrays of strings via KN_cinterop`() {
        assertNull(messageQu)
        createQueue()
        assertNotNull(messageQu)
    }

    /**all features is provided by drill-agent/src/nativeInterop/cinterop/drillInternal.def file.*/
    @Test
    fun `working with ws messages queue`() {
        createQueue()
        val messageForQueue = "test message"
        addMessage(messageForQueue.cstr.getPointer(Arena()))
        assertEquals(getMessage()?.toKString(), messageForQueue)
    }


    companion object {
        const val ARGUMENT_TEST_DATA = "xx=123,sad=jjk"
    }
}


