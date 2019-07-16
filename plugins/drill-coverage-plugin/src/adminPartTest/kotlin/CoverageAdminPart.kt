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

    private val agentState = agentStates[agentInfo.id]!!

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
            val (name, bytes) = agentData.classData.first()
            assertEquals(dummyClass.path, name)
            assertTrue { dummyBytes.contentEquals(bytes) }
        }
    }

    @Test
    fun `should send messages to WebSocket on empty data`() {
        prepareClasses(Dummy::class.java)
        val sessionId = "xxx"
        sendMessage(SessionStarted(sessionId, "", currentTimeMillis()))
        val finished = SessionFinished(sessionId, currentTimeMillis())
        sendMessage(finished)
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
        val sessionId = "xxx"
        
        val started = SessionStarted(sessionId, "", currentTimeMillis())

        sendMessage(started)

        val finished = SessionFinished(sessionId, currentTimeMillis())

        sendMessage(finished)

        assertNotNull(JavaPackageCoverage)

        val methods = ws.javaPackagesCoverage.flatMap { it.classes }.flatMap { it.methods }

        assertEquals(countClassesInPackage, ws.javaPackagesCoverage.first().classes.size)
        assertEquals("Dummy", ws.javaPackagesCoverage.first().classes.first().name)
        assertEquals(countPackages, ws.javaPackagesCoverage.size)
        assertEquals(countAllClasses, ws.javaPackagesCoverage.count { it.classes.isNotEmpty() })
        assertEquals(countAllMethods, methods.size)
    }

    @Test
    fun `empty activeScope should not be saved during switch to new scope`() {
        runBlocking { coverageController.changeActiveScope(ActiveScopeChangePayload("testScope")) }
        assertEquals("testScope", agentState.activeScope.name)
        runBlocking {
            coverageController.changeActiveScope(ActiveScopeChangePayload("testScope2", true))
        }
        assertNull(agentState.scopes["testScope"])
    }

    @Test
    fun `not empty activeScope should switch to a specified one with previous scope deletion`() {
        runBlocking { coverageController.changeActiveScope(ActiveScopeChangePayload("testScope")) }
        assertEquals("testScope", agentState.activeScope.name)
        prepareClasses()
        appendSessionStub(agentState, agentState.classesData())
        runBlocking { coverageController.changeActiveScope(ActiveScopeChangePayload("testScope2")) }
        assertNull(agentState.scopes["testScope"])
    }

    @Test
    fun `not empty activeScope should switch to a specified one with saving previous scope`() {
        runBlocking { coverageController.changeActiveScope(ActiveScopeChangePayload("testScope")) }
        assertEquals("testScope", agentState.activeScope.name)
        prepareClasses()
        appendSessionStub(agentState, agentState.classesData())
        runBlocking {
            coverageController.changeActiveScope(ActiveScopeChangePayload("testScope2", true))
        }
        assertTrue { agentState.scopes.values.any { it.name == "testScope" } }
    }

    private fun appendSessionStub(agentState: AgentState, classesData: ClassesData) {
        agentState.activeScope.update(
            FinishedSession(
                "testSession",
                "MANUAL",
                mapOf()
            ),
            classesData
        )
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
        val classBytes = ClassBytes(clazz.path, bytes.encode())
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



