package com.epam.drill.core

import com.soywiz.klogger.Logger
import com.soywiz.korio.async.await
import drillInternal.*
import kotlinx.cinterop.Arena
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import storage.loggers
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
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

    @Test
    fun `init globals should create an raw shared pointers`() {
        val testInstallationDir = "test"
        initCGlobals(CConfig(testInstallationDir))
        assertNotNull(config.di)
        assertNotNull(config.drillInstallationDir)
        //alias
        assertEquals(drillInstallationDir, testInstallationDir)
    }

    @Test
    fun `initLoggers function should create an raw shared pointer to empty map`() {
        initLoggers()
        assertNotNull(loggers.logs)
        assertTrue { loggers.logs?.asStableRef<MutableMap<String, Logger>>()?.get()?.isEmpty() ?: false }
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

    /**all features is provided by drill-agent/src/nativeInterop/cinterop/drillInternal.def file.*/
    //fixme move it to sender
    @Test
    fun `Queue for ws messages should be synchronized`() = runBlocking {
        val threadNumber = 100

        createQueue()
        repeat(2) {
            List(threadNumber) {
                Worker.start(true).execute(TransferMode.UNSAFE, {}) {
                    val messageForQueue = "test message"
                    addMessage(messageForQueue.cstr.getPointer(Arena()))
                }
            }.forEach { it.await() }
            repeat(threadNumber) {
                assertNotNull(getMessage()?.toKString())
            }
        }

    }

    companion object {
        const val ARGUMENT_TEST_DATA = "xx=123,sad=jjk"
    }
}