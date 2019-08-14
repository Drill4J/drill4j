@file:Suppress("UNUSED_PARAMETER")

package com.epam.drill.endpoints

import com.epam.drill.*
import com.epam.drill.cache.*
import com.epam.drill.cache.impl.*
import com.epam.drill.common.*
import com.epam.drill.endpoints.plugin.*
import com.epam.drill.storage.*
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.junit.*
import org.junit.Test
import org.kodein.di.generic.*
import kotlin.coroutines.*
import kotlin.test.*


@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
class DrillPluginWsTest {

    @ExperimentalCoroutinesApi
    @KtorExperimentalLocationsAPI
    @KtorExperimentalAPI
    companion object {
        var engine: TestApplicationEngine? = null
        var wsPluginService: DrillPluginWs? = null
        val agentStorage = AgentStorage()
        val agentId = "testAgent"
        val buildVersion = "1.0.0"
        val agentInfo = AgentInfo(
            id = agentId,
            name = "test",
            status = AgentStatus.READY,
            ipAddress = "1.7.2.23",
            groupName = "test",
            description = "test",
            buildVersion = buildVersion,
            buildAlias = "testAlias"
        )

        @BeforeClass
        @JvmStatic
        fun initTestEngine() {
            engine = TestApplicationEngine(createTestEnvironment())
            engine!!.start(wait = false)
            installation = {
                install(WebSockets)
                install(Locations)
            }
            kodeinConfig = {
                bind<DrillPluginWs>() with eagerSingleton {
                    wsPluginService = DrillPluginWs(kodein)
                    wsPluginService!!
                }
                bind<WsTopic>() with singleton { WsTopic(kodein) }
                bind<CacheService>() with eagerSingleton { HazelcastCacheService() }
                bind<AgentStorage>() with eagerSingleton { agentStorage }
                bind<AgentManager>() with eagerSingleton { AgentManager(kodein) }
            }
            engine!!.application.module()
        }
    }

    @Test
    fun `should retrun CloseFrame if we subscribe without SubscribeInfo`() {
        with(engine) {
            this?.handleWebSocketConversation("/ws/drill-plugin-socket") { incoming, outgoing ->
                outgoing.send(message(MessageType.SUBSCRIBE, "/pluginTopic1", ""))
                val receive = incoming.receive()
                assertTrue(receive is Frame.Close)
                assertEquals(CloseReason.Codes.UNEXPECTED_CONDITION.code, receive.readReason()?.code)
            }
        }
    }


    @Test
    fun `should communicate with pluginWs and return the empty MESSAGE`() {
        with(engine) {
            this?.handleWebSocketConversation("/ws/drill-plugin-socket") { incoming, outgoing ->
                val destination = "/pluginTopic2"
                outgoing.send(
                    message(
                        MessageType.SUBSCRIBE,
                        destination,
                        SubscribeInfo.serializer() stringify SubscribeInfo(agentId, buildVersion)
                    )
                )
                val receive = incoming.receive() as? Frame.Text ?: fail()
                val readText = receive.readText()
                val fromJson = Message.serializer() parse readText
                assertEquals(destination, fromJson.destination)
                assertEquals(MessageType.MESSAGE, fromJson.type)
                assertTrue { fromJson.message.isEmpty() }
            }
        }
    }

    @Test
    fun `should return data from storage which was sent before via send()`() {
        with(engine) {
            this?.handleWebSocketConversation("/ws/drill-plugin-socket") { incoming, outgoing ->
                val destination = "/pluginTopic1"
                val messageForTest = "testMessage"
                wsPluginService?.send(agentInfo, destination, messageForTest)
                outgoing.send(
                    message(
                        MessageType.SUBSCRIBE,
                        destination,
                        SubscribeInfo.serializer() stringify SubscribeInfo(agentId, buildVersion)
                    )
                )

                val receive = incoming.receive() as? Frame.Text ?: fail()
                val readText = receive.readText()
                val fromJson = Message.serializer() parse readText
                assertEquals(destination, fromJson.destination)
                assertEquals(MessageType.MESSAGE, fromJson.type)
                assertEquals(messageForTest, fromJson.message)

                outgoing.send(message(MessageType.SUBSCRIBE, destination, ""))
            }
        }
    }

    @Test
    fun `should return data from storage for current buildVersion if BV is null`() {
        with(engine) {
            this?.handleWebSocketConversation("/ws/drill-plugin-socket") { incoming, outgoing ->
                val destination = "/pluginTopic1"
                val messageForTest = "testMessage"
                agentStorage.put(
                    agentInfo.id, AgentEntry(
                        agentInfo,
                        DefWebSocketSessionStub()
                    )
                )
                wsPluginService?.send(agentInfo, destination, messageForTest)
                outgoing.send(
                    message(
                        MessageType.SUBSCRIBE,
                        destination,
                        SubscribeInfo.serializer() stringify SubscribeInfo(agentId, null)
                    )
                )

                val receive = incoming.receive() as? Frame.Text ?: fail()
                val readText = receive.readText()
                val fromJson = Message.serializer() parse readText
                assertEquals(destination, fromJson.destination)
                assertEquals(MessageType.MESSAGE, fromJson.type)
                assertEquals(messageForTest, fromJson.message)

                outgoing.send(message(MessageType.SUBSCRIBE, destination, ""))
            }
        }
    }
}

class DefWebSocketSessionStub : DefaultWebSocketSession {

    override val closeReason: Deferred<CloseReason?>
        get() = TODO("not implemented")
    override val coroutineContext: CoroutineContext
        get() = TODO("not implemented")
    override val incoming: ReceiveChannel<Frame>
        get() = TODO("not implemented")
    override var masking: Boolean
        get() = TODO("not implemented")
        set(value) {}
    override var maxFrameSize: Long
        get() = TODO("not implemented")
        set(value) {}
    override val outgoing: SendChannel<Frame>
        get() = TODO("not implemented")
    override var pingIntervalMillis: Long
        get() = TODO("not implemented")
        set(value) {}
    override var timeoutMillis: Long
        get() = TODO("not implemented")
        set(value) {}

    @KtorExperimentalAPI
    override suspend fun close(cause: Throwable?) {
    }

    override suspend fun flush() {}
    override fun terminate() {}
}
