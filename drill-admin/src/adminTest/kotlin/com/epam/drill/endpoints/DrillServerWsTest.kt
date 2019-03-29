package com.epam.drill.endpoints

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.module
import com.epam.drill.wsHandlers
import io.ktor.application.install
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.Test
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.singleton
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@KtorExperimentalLocationsAPI
@ObsoleteCoroutinesApi
internal class DrillServerWsTest {
    @Test
    fun testConversation() {
        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        val pluginStorage = HashSet<DrillWsSession>()
        engine.application.module({
            install(WebSockets)
            install(Locations)
        }, {
            import(wsHandlers)
            bind<WsTopic>() with singleton { WsTopic(kodein) }
            bind<MutableSet<DrillWsSession>>() with eagerSingleton { pluginStorage }

        })

        with(engine) {
            handleWebSocketConversation("/ws/drill-admin-socket") { incoming, outgoing ->
                outgoing.send(Message(MessageType.SUBSCRIBE, "/mytopic", "").textFrame())
                assertNotNull(incoming.receive())
                assertEquals(1, pluginStorage.size)
                outgoing.send(Message(MessageType.UNSUBSCRIBE, "/mytopic", "").textFrame())
                outgoing.send(Message(MessageType.SUBSCRIBE, "/mytopic", "").textFrame())
                assertNotNull(incoming.receive())
                assertEquals(1, pluginStorage.size)
                outgoing.send(Message(MessageType.SUBSCRIBE, "/mytopic2", "").textFrame())
                assertNotNull(incoming.receive())
                assertEquals(2, pluginStorage.size)
            }
        }

    }
}