package com.epam.drill.endpoints

import com.epam.drill.*
import com.epam.drill.cache.*
import com.epam.drill.cache.impl.*
import com.epam.drill.common.*
import com.epam.drill.endpoints.agent.*
import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.junit.Test
import org.kodein.di.*
import org.kodein.di.generic.*
import kotlin.test.*

@ExperimentalCoroutinesApi
@KtorExperimentalLocationsAPI
internal class DrillServerWsTest {

    @Test
    fun testConversation() {
        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        val pluginStorage = HashSet<DrillWsSession>()
        installation = {
            install(WebSockets)
            install(Locations)
        }
        kodeinConfig = {
            import(wsHandlers)
            bind<WsTopic>() with singleton { WsTopic(kodein) }
            bind<MutableSet<DrillWsSession>>() with eagerSingleton { pluginStorage }
            bind<CacheService>() with eagerSingleton { HazelcastCacheService() }

        }
        engine.application.module()

        with(engine) {
            handleWebSocketConversation("/ws/drill-admin-socket") { incoming, outgoing ->
                outgoing.send(message(MessageType.SUBSCRIBE, "/mytopic", ""))
                assertNotNull(incoming.receive())
                assertEquals(1, pluginStorage.size)
                outgoing.send(message(MessageType.UNSUBSCRIBE, "/mytopic", ""))
                outgoing.send(message(MessageType.SUBSCRIBE, "/mytopic", ""))
                assertNotNull(incoming.receive())
                assertEquals(1, pluginStorage.size)
                outgoing.send(message(MessageType.SUBSCRIBE, "/mytopic2", ""))
                assertNotNull(incoming.receive())
                assertEquals(2, pluginStorage.size)
                assertEquals(2, pluginStorage.map { it.url }.toSet().size)
            }
        }

    }

    @Test
    fun `universal serialization proceeds correctly`() {
        val empty = serialize(null)
        assertEquals("", empty)
        val string = "someText: \"asdf\""
        val serializedString = serialize(string)
        assertEquals(string, serializedString)
        val complexStructure1 = mapOf("key" to Message(MessageType.SUBSCRIBE, "asd", "vbn"))
        val serializedStructure1 = serialize(complexStructure1)
        val result1 = "{\"key\":{\"type\":\"SUBSCRIBE\",\"destination\":\"asd\",\"message\":\"vbn\"}}"
        assertEquals(result1, serializedStructure1)
        val complexStructure2 = arrayListOf(PluginConfig("1", "2"))
        val serializedStructure2 = serialize(complexStructure2)
        val result2 = "[{\"id\":\"1\",\"data\":\"2\"}]"
        assertEquals(result2, serializedStructure2)
    }

    @Test
    fun `topic resolvation goes correctly`() {
        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        val pluginStorage = HashSet<DrillWsSession>()
        installation = {
            install(WebSockets)
            install(Locations)
        }
        val wsHandlers = Kodein.Module(name = "wsHandlers") {
            bind<DrillServerWs>() with eagerSingleton {
                DrillServerWs(
                    kodein
                )
            }
        }
        kodeinConfig = {
            import(wsHandlers, allowOverride = true)
            bind<WsTopic>() with singleton { WsTopic(kodein) }
            bind<MutableSet<DrillWsSession>>() with eagerSingleton { pluginStorage }
            bind<CacheService>() with eagerSingleton { HazelcastCacheService() }
            bind<ServerStubTopics>() with eagerSingleton { ServerStubTopics(kodein) }
        }
        engine.application.module()
        with(engine) {
            handleWebSocketConversation("/ws/drill-admin-socket") { incoming, outgoing ->
                outgoing.send(message(MessageType.SUBSCRIBE, "/pathOfPain", ""))
                val tmp = incoming.receive()
                //val response = (tmp as Frame.Text).readText()
                //println("RESPONSE: $response")
                assertNotNull(tmp)
            }
        }
    }
}

fun message(type: MessageType, destination: String, message: String) =
    (Message.serializer() stringify Message(type, destination, message)).textFrame()


class ServerStubTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()

    init {
        runBlocking {
            wsTopic {
                topic<PainRoutes.SomeData> { payload, _ ->
                    "the data is: ${payload.data}"
                }
            }

        }
    }
}

object PainRoutes {
    @Location("/pathOfPain")
    data class SomeData(val data: String)
}