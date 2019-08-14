package com.epam.drill.plugins.coverage

import com.epam.drill.common.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import com.epam.drill.plugins.coverage.test.bar.*
import com.epam.drill.plugins.coverage.test.foo.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.jacoco.core.internal.data.*
import java.util.concurrent.*
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
        assertTrue { agentData is ClassDataBuilder }
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
            assertTrue { dummyBytes contentEquals bytes }
        }
    }

    @Test
    fun `should send messages to WebSocket on empty data`() {
        prepareClasses(Dummy::class.java)
        val sessionId = "xxx"
        sendMessage(SessionStarted(sessionId, "", currentTimeMillis()))
        val finished = SessionFinished(sessionId, currentTimeMillis())
        sendMessage(finished)
        assertTrue { ws.sent["/build/methods"] != null }
        assertTrue { ws.sent["/build/coverage"] != null }
        assertTrue { ws.sent["/build/coverage-by-packages"] != null }
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
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testScope"))) }
        assertEquals("testScope", agentState.activeScope.name)
        runBlocking {
            coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testScope2", true)))
        }
        assertNull(agentState.scopes["testScope"])
    }

    @Test
    fun `not empty activeScope should switch to a specified one with previous scope deletion`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testScope"))) }
        assertEquals("testScope", agentState.activeScope.name)
        appendSessionStub(agentState, agentState.classesData())
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testScope2"))) }
        assertNull(agentState.scopes["testScope"])
    }

    @Test
    fun `not empty activeScope should switch to a specified one with saving previous scope`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.changeActiveScope(ActiveScopeChangePayload("testScope66")) }
        assertEquals("testScope66", agentState.activeScope.name)
        appendSessionStub(agentState, agentState.classesData())
        runBlocking {
            coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testScope6", true)))
        }
        assertTrue { agentState.scopes.values.any { it.name == "testScope66" } }
    }

    @Test
    fun `DropScope action deletes the specified scope data from storage`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testDropScope"))) }
        appendSessionStub(agentState, agentState.classesData())
        runBlocking {
            coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("testDropScope2", true)))
        }
        val id = agentState.scopes.values.find { it.name == "testDropScope" }?.id
        assertNotNull(id)
        runBlocking { coverageController.doAction(DropScope(ScopePayload(id))) }
        val deleted = agentState.scopes.values.find { it.id == id }
        assertNull(deleted)
    }

    @Test
    fun `active scope renaming process goes correctly`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("renameActiveScope1"))) }
        assertEquals(agentState.activeScope.summary.name, "renameActiveScope1")
        val activeId = agentState.activeScope.id
        runBlocking { coverageController.doAction(RenameScope(RenameScopePayload(activeId, "renameActiveScope2"))) }
        assertEquals(agentState.activeScope.summary.name, "renameActiveScope2")
    }

    @Test
    fun `finished scope renaming process goes correctly`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("renameFinishedScope1"))) }
        appendSessionStub(agentState, agentState.classesData())
        runBlocking {
            coverageController.doAction(
                SwitchActiveScope(
                    ActiveScopeChangePayload(
                        "renameFinishedScope2",
                        true
                    )
                )
            )
        }
        val finishedId = agentState.scopes.values.find { it.name == "renameFinishedScope1" }?.id!!
        runBlocking { coverageController.doAction(RenameScope(RenameScopePayload(finishedId, "renamedScope1"))) }
        val renamed = agentState.scopes[finishedId]!!
        assertEquals(renamed.name, "renamedScope1")
    }

    @Test
    fun `neither active nor finished scope can be renamed to an existing scope name`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("occupiedName1"))) }
        appendSessionStub(agentState, agentState.classesData())
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("occupiedName2", true))) }
        appendSessionStub(agentState, agentState.classesData())
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("freeName", true))) }
        val finishedId = agentState.scopes.values.find { it.name == "occupiedName1" }?.id!!
        runBlocking { coverageController.doAction(RenameScope(RenameScopePayload(finishedId, "occupiedName2"))) }
        assertEquals(agentState.scopes[finishedId]!!.name, "occupiedName1")
        val activeId = agentState.activeScope.id
        runBlocking { coverageController.doAction(RenameScope(RenameScopePayload(activeId, "occupiedName2"))) }
        assertEquals(agentState.activeScope.summary.name, "freeName")
    }

    @Test
    fun `not possible to switch scope to a new one with already existing name`() {
        prepareClasses(Dummy::class.java)
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("occupiedName"))) }
        appendSessionStub(agentState, agentState.classesData())
        val activeId1 = agentState.activeScope.id
        runBlocking { coverageController.doAction(SwitchActiveScope(ActiveScopeChangePayload("occupiedName", true))) }
        val activeId2 = agentState.activeScope.id
        assertEquals(activeId1, activeId2)
    }

    @Test
    fun `should compute new methods coverage rates`() {
        runBlocking {
            prepareClasses(Dummy::class.java)
            commendTestSession(listOf(true, false))
            val methods = ws.sent["/scope/${agentState.activeScope.id}/methods"]
            @Suppress("UNCHECKED_CAST")
            val parsed = BuildMethods.serializer() parse (methods as String)
            assertTrue { parsed.totalMethods.methods.any { it.coverageRate == CoverageRate.FULL } }
            assertTrue { parsed.totalMethods.methods.any { it.coverageRate == CoverageRate.MISSED } }
        }
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

    private fun commendTestSession(probes: List<Boolean>) {
        val sessionId = "test"
        val className = "com/epam/drill/plugins/coverage/Dummy"
        sendMessage(SessionStarted(sessionId, "MANUAL", currentTimeMillis()))
        sendMessage(
            CoverDataPart(
                sessionId,
                listOf(
                    ExecClassData(
                        id = CRC64.classId(agentState.classesData().classesBytes[className]),
                        className = className,
                        probes = probes,
                        testName = "someTestName"
                    )
                )
            )
        )
        sendMessage(SessionFinished(sessionId, currentTimeMillis()))
    }
}

class SenderStub : Sender {

    val sent = ConcurrentHashMap<String, Any>()

    lateinit var javaPackagesCoverage: List<JavaPackageCoverage>

    override suspend fun send(agentInfo: AgentInfo, destination: String, message: String) {
        if (!message.isEmpty()) {
            sent[destination] = message
            if (destination.endsWith("/coverage-by-packages"))
                javaPackagesCoverage = JavaPackageCoverage.serializer().list parse message
        }
    }
}
