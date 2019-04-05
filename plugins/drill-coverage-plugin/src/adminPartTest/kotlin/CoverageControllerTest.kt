package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoverageControllerTest {

    private val ws = WsServiceStub()

    private val coverageController = CoverageController(ws, "test")

    @Test
    fun init() {
        assertTrue { coverageController.initialClassBytes.isEmpty() }
        assertTrue { coverageController.javaClasses.isEmpty() }
        assertTrue { coverageController.prevJavaClasses.isEmpty() }
    }

    @Test
    fun `process init message`() {
        val message = CoverageMessage(CoverageEventType.INIT, "hello")

        coverageController.javaClasses["TestClass"] = JavaClass("TestClass", "TestClass", emptySet())

        runBlocking {
            coverageController.processData(DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message)))
        }

        coverageController.apply {
            assertTrue { initialClassBytes.isEmpty() }
            assertTrue { javaClasses.isEmpty() }
            assertTrue { prevJavaClasses.isNotEmpty() }
        }
    }

    @Test
    fun `receive class`() {
        val clazz = Dummy::class.java
        val bytes = clazz.readBytes()
        prepareClasses(clazz)

        coverageController.apply {
            val path = clazz.path
            assertNotNull(initialClassBytes[path])
            assertTrue { initialClassBytes[path]!!.contentEquals(bytes) }
            assertNotNull(javaClasses[path])
        }
    }

    @Test
    fun `empty coverage`() {
        prepareClasses(Dummy::class.java)
        val message = CoverageMessage(CoverageEventType.COVERAGE_DATA, "[]")


        runBlocking {
            coverageController.processData(DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message)))
        }
        assertTrue { ws.sent.any { it.first == "/coverage-new" } }
        assertTrue { ws.sent.any { it.first == "/coverage" } }
    }

    private fun prepareClasses(vararg classes: Class<*>) {
        for (clazz in classes) {
            val bytes = clazz.readBytes()
            val classBytes = ClassBytes(clazz.path, bytes.toList())
            val messageString = JSON.stringify(ClassBytes.serializer(), classBytes)
            val message = CoverageMessage(CoverageEventType.CLASS_BYTES, messageString)

            runBlocking {
                coverageController.processData(DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message)))
            }
        }
    }
}

class WsServiceStub : WsService {

    val sent = mutableListOf<Pair<String, Any>>()

    override suspend fun convertAndSend(destination: String, message: Any) {
        sent.add(destination to message)
    }

    override fun getPlWsSession() = setOf<String>()
}


fun Class<*>.readBytes() = this.getResourceAsStream("/${this.path}.class").readBytes()

val Class<*>.path get() = this.name.replace('.', '/')

