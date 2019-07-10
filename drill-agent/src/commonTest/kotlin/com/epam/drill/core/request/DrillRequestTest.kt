package com.epam.drill.core.request

import com.epam.drill.plugin.*
import kotlin.test.*


class DrillRequestTest {
    private val drillSessionId =
        "RIBpwqEbmpA6ZuoPAPhW8VaAo/bab3KPxjhytpByq6qrcmN2Ia0TPJtPptzKy8A8QhX1gVI2EUeILknUq4sMdQ=="
    private val headers = mutableMapOf(
        "Host" to "myhost.epam.com",
        "Connection" to "keep-alive",
        "Cache-Control" to "max-age=0",
        "Upgrade-Insecure-Requests" to "1",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
        "Referer" to "https://myhost.epam.com/"
    )
    private val cookies = mutableMapOf(
        "login" to "",
        "_ga" to "GA1.2.1853615443.1549016603",
        "LWSSO_COOKIE_KEY" to "4nyn6sG97uQIOZO84iBPzmh-pt4gzY6oZzrY3Sx8MnYwweTjwrLX84rpguTHDIeAPueinNobGLO8Spv2n3U-ej8TamQ8Zb6xrLMss4SDZWdB2Lr0-qjzjmrE0KYJIOUpKrxcFYb3ykldAo_FdqSFFw2fd3DjX6GyodE4l_lTuiZiry_kYsPKLIZQywVh_Yvas0-_WVsXsDrHrnv2OJkRdA..",
        "_gid" to "GA1.2.1089535717.1554294889",
        "fkey" to "b54a2dbf-3193-4b06-b8d6-531284cd6870"
    )

    private fun constructRequest(drillSessionInHeader: Boolean = false, drillSessionInCookie: Boolean = false): String {
        if (drillSessionInHeader)
            headers += Pair("drill-session-id", drillSessionId)
        if (drillSessionInCookie)
            cookies += Pair("drill-session-id", drillSessionId)

        return HttpRequest(
            "GET /test/url HTTP/1.1".toRequestQuery(),
            headers,
            cookies
        ).toRawRequestString()

    }

    @Test
    fun `should parse a raw HTTP10 request and extract the drill request`() {
        val drillRequest = parseHttpRequest(constructRequest())
        assertNotNull(drillRequest)
    }

    @Test
    fun `should extract the drillSessionId from a raw HTTP10 request cookies`() {
        val drillRequest = parseHttpRequest(constructRequest(drillSessionInCookie = true)).toDrillRequest()
        assertEquals(drillRequest.drillSessionId, drillSessionId)
    }

    @Test
    fun `should extract the drillSessionId from a raw HTTP10 request headers`() {
        val drillRequest = parseHttpRequest(constructRequest(drillSessionInHeader = true)).toDrillRequest()
        assertEquals(drillRequest.drillSessionId, drillSessionId)
    }

}