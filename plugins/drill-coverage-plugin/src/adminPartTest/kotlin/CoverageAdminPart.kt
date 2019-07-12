package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import com.epam.drill.plugins.coverage.test.bar.*
import com.epam.drill.plugins.coverage.test.foo.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.test.*

class CoverageAdminPartTest {
    private val agentInfo = AgentInfo(
        id = "id",
        name = "test",
        status = AgentStatus.READY,
        groupName = "test",
        description = "test",
        ipAddress = "127.0.0.1",
        buildVersion = "1.0.1",
        buildAlias = "test alias",
        buildVersions = mutableSetOf()
    )
    private val ws = SenderStub()

    private val coverageController = CoverageAdminPart(ws, agentInfo, "test")

    @Test
    fun `should have some state before init`() {
        assertTrue { agentStates.isNotEmpty() }
    }

    @Test
    fun `should switch agent data ref to ClassDataBuilder on init`() = runBlocking {
        val initInfo = InitInfo(1, "hello")

        sendMessage(initInfo)

        assertEquals(1, agentStates.count())
        val agentData = agentStates[agentInfo.id]?.data
        assertTrue { agentData is ClassDataBuilder && agentData.count == initInfo.classesCount }
    }

    @Test
    fun `should add class bytes on receiving a CLASS_BYTES message`() = runBlocking {
        val dummyClass = Dummy::class.java
        val dummyBytes = dummyClass.readBytes()

        sendInit(dummyClass)
        sendClass(dummyClass)

        agentStates[agentInfo.id]!!.run {
            val agentData = data as ClassDataBuilder
            val (name, bytes) = agentData.classData.poll()!!
            assertEquals(dummyClass.path, name)
            assertTrue { dummyBytes.contentEquals(bytes) }
        }
    }

    @Test
    fun `should send messages to WebSocket on empty data`() {
        prepareClasses(Dummy::class.java)
        val message = SessionFinished(ts = System.currentTimeMillis())
        sendMessage(message)
        assertTrue { ws.sent.any { it.first == "/coverage-new" } }
        assertTrue { ws.sent.any { it.first == "/coverage" } }
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
        val message = SessionFinished(ts = System.currentTimeMillis())

        sendMessage(message)

        assertNotNull(JavaPackageCoverage)

        val methods = ws.javaPackagesCoverage.flatMap { it.classes }.flatMap { it.methods }

        assertEquals(countClassesInPackage, ws.javaPackagesCoverage.first().classes.size)
        assertEquals("Dummy", ws.javaPackagesCoverage.first().classes.first().name)
        assertEquals(countPackages, ws.javaPackagesCoverage.size)
        assertEquals(countAllClasses, ws.javaPackagesCoverage.count { it.classes.isNotEmpty() })
        assertEquals(countAllMethods, methods.size)
    }

    private fun prepareClasses(vararg classes: Class<*>) {
        runBlocking {
            sendInit(*classes)
            for (clazz in classes) {
                sendClass(clazz)
            }
            sendMessage(Initialized("Initialized!"))
        }
    }

    private fun sendClass(clazz: Class<*>) {
        val bytes = clazz.readBytes()
        val classBytes = ClassBytes(clazz.path, bytes.toList())
        sendMessage(classBytes)
    }

    private fun sendInit(vararg classes: Class<*>) {
        val initInfo = InitInfo(classes.count(), "Start initialization")
        sendMessage(initInfo)
    }

    private fun sendMessage(message: CoverMessage) {
        val messageStr = commonSerDe.stringify(CoverMessage.serializer(), message)
        runBlocking {
            coverageController.processData(DrillMessage("", messageStr))
        }
    }
}

class SenderStub : Sender {

    val sent = mutableListOf<Pair<String, Any>>()

    lateinit var javaPackagesCoverage: List<JavaPackageCoverage>

    override suspend fun send(agentInfo: AgentInfo, destination: String, message: String) {
        sent.add(destination to message)
        if (destination == "/coverage-by-packages")
            javaPackagesCoverage = JavaPackageCoverage.serializer().list parse message
    }
}



