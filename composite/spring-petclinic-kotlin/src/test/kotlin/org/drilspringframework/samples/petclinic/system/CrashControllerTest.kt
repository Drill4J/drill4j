package org.drilspringframework.samples.petclinic.system

import drl.test.CrashController
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.socket.PortFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.web.util.NestedServletException

@RunWith(SpringRunner::class)
@WebMvcTest(controllers = [(CrashController::class)])
class CrashControllerTest {

    @Autowired
    private val mockMvc: MockMvc? = null

    @Before
    fun setUp() {
        ClientAndServer(PortFactory.findFreePort()).`when`(HttpRequest.request()
                .withMethod("POST")
                .withPath("/plugin/pluginDispatcher"))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withHeaders(Header("content-type", "application/json"))
                        .withBody("super")
                )

        println()
    }

    @Test(expected = NestedServletException::class)
    @Throws(Exception::class)
    fun checkPluginLoader() {

        mockMvc!!.perform(get("/oups")).andReturn().response.contentAsString
    }

}
