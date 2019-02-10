package org.drilspringframework.samples.petclinic.system

import org.junit.Assert.assertNotNull
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@RunWith(SpringRunner::class)
@WebMvcTest(controllers = [(WelcomeController::class)])
class ServletDrilLInjectingTest {

    @Autowired
    private val mockMvc: MockMvc? = null

    @Test
    @Throws(Exception::class)
    fun checkInjectedScripts() {
        Assert.assertTrue(mockMvc!!.perform(get("/")).andReturn().response.contentAsString.contains("id=drill-pane"))
    }

    @Test
    @Throws(Exception::class)
    fun checkDrillCookies() {
        val response = mockMvc!!.perform(get("/")).andReturn().response
        assertNotNull(response.getCookie("DrillHttpHost"))
        assertNotNull(response.getCookie("DrillSessionId"))
        assertNotNull(response.getCookie("DrillSocketHost"))
    }

}
