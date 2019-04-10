package com.epam.drill.plugins.coverage

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoverageControllerTest {
    private val agentInfo = AgentInfo(
        id = "id",
        name = "test",
        groupName = "test",
        description = "test",
        ipAddress = "127.0.0.1",
        isEnable = true,
        buildVersion = "1.0.1"
    )
    private val ws = WsServiceStub()

    private val coverageController = CoverageController(ws, "test")

    @Test
    fun `should have empty state after init`() {
        assertTrue { coverageController.initialClassBytes.isEmpty() }
        assertTrue { coverageController.javaClasses.isEmpty() }
        assertTrue { coverageController.prevJavaClasses.isEmpty() }
    }

    @Test
    fun `should preserve class data for diff`() {
        val message = CoverageMessage(CoverageEventType.INIT, "hello")

        coverageController.javaClasses["TestClass"] = JavaClass("TestClass", "TestClass", emptySet())

        runBlocking {
            coverageController.processData(
                agentInfo,
                DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message))
            )
        }

        coverageController.apply {
            assertTrue { initialClassBytes.isEmpty() }
            assertTrue { javaClasses.isEmpty() }
            assertTrue { prevJavaClasses.isNotEmpty() }
        }
    }

    @Test
    fun `should add class bytes on receiving a CLASS_BYTES message`() {
        val clazz = Dummy::class.java
        val bytes = clazz.readBytes()
        prepareClasses(clazz)

        coverageController.apply {
            val path = clazz.path
            assertNotNull(initialClassBytes[path])
            assertTrue { bytes.contentEquals(initialClassBytes[path]!!) }
        }
    }

    @Test
    fun `should send messages to WebSocket on empty data`() {
        prepareClasses(Dummy::class.java)
        val message = CoverageMessage(CoverageEventType.COVERAGE_DATA, "[]")


        runBlocking {
            coverageController.processData(
                agentInfo,
                DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message))
            )
        }
        assertTrue { ws.sent.any { it.first == "/coverage-new" } }
        assertTrue { ws.sent.any { it.first == "/coverage" } }
        assertTrue { ws.sent.any { it.first == "/coverage-by-classes" } }
        assertTrue { ws.sent.any { it.first == "/coverage-by-packages" } }
    }

    private fun prepareClasses(vararg classes: Class<*>) {
        for (clazz in classes) {
            val bytes = clazz.readBytes()
            val classBytes = ClassBytes(clazz.path, bytes.toList())
            val messageString = JSON.stringify(ClassBytes.serializer(), classBytes)
            val message = CoverageMessage(CoverageEventType.CLASS_BYTES, messageString)

            runBlocking {
                coverageController.processData(
                    agentInfo,
                    DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message))
                )
            }
        }
    }
}

class WsServiceStub : WsService {

    val sent = mutableListOf<Pair<String, Any>>()

    override suspend fun convertAndSend(agentInfo: AgentInfo, destination: String, message: String) {
        sent.add(destination to message)
    }

    override fun getPlWsSession() = setOf<String>()
}


fun Class<*>.readBytes() = this.getResourceAsStream("/${this.path}.class").readBytes()

val Class<*>.path get() = this.name.replace('.', '/')

