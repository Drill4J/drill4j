package com.epam.drill.plugins.coverage

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import com.epam.drill.plugins.coverage.test.bar.BarDummy
import com.epam.drill.plugins.coverage.test.foo.FooDummy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlin.test.*

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

    @Test
    fun `should preserve coverage for packages`() {
        // Count of Classes in package for test
        val countClassesInPackage = 1
        // Count of packages for test
        val countPackages = 3
        // Total count of Classes for test
        val countAllClasses = 3
        // Total count of Methods for test
        val countAllMethods = 6

        prepareClasses(Dummy::class.java, BarDummy::class.java, FooDummy::class.java)
        val message = CoverageMessage(CoverageEventType.COVERAGE_DATA, "[]")

        runBlocking {
            coverageController.processData(
                agentInfo,
                DrillMessage("", JSON.stringify(CoverageMessage.serializer(), message))
            )
        }

        assertNotNull(JavaPackageCoverage)

        val methods = ws.javaPackagesCoverage.flatMap { it.classes }.flatMap { it.methods }

        assertEquals(countClassesInPackage, ws.javaPackagesCoverage.first().classes.size)
        assertEquals("Dummy",  ws.javaPackagesCoverage.first().classes.first().name)
        assertEquals(countPackages, ws.javaPackagesCoverage.size)
        assertEquals(countAllClasses, ws.javaPackagesCoverage.count { it.classes.isNotEmpty() })
        assertEquals(countAllMethods, methods.size)
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

    lateinit var javaPackagesCoverage: List<JavaPackageCoverage>

    override suspend fun convertAndSend(agentInfo: AgentInfo, destination: String, message: String) {
        sent.add(destination to message)
        if (destination == "/coverage-by-packages")
            javaPackagesCoverage = JSON.parse(JavaPackageCoverage.serializer().list, message)
    }

    override fun getPlWsSession() = setOf<String>()
}


fun Class<*>.readBytes() = this.getResourceAsStream("/${this.path}.class").readBytes()

val Class<*>.path get() = this.name.replace('.', '/')

