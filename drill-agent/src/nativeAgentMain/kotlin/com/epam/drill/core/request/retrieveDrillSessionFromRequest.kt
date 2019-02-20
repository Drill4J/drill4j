package com.epam.drill.core.request


object RetrieveDrillSessionFromRequest {

    operator fun invoke(request: String): DrillRequest {
        val requestHeaders = mutableMapOf<String, String>()
        val cookies = mutableMapOf<String, String>()
        val reader = request.lineSequence().iterator()
        // Request-Line ; Section 5.1
        reader.next()
        var header: String? = reader.next()
        if (header != null) {
            while (reader.hasNext() && header!!.isNotEmpty()) {
                val pr = parseHeaderLine(header)
                requestHeaders[pr.first] = pr.second
                header = reader.next()
            }
            if (header != null && header.isNotEmpty()) {
                val x = parseHeaderLine(header)
                requestHeaders[x.first] = x.second
            }
        }
        requestHeaders.forEach { k->
            println(k.key+" _"+k.value)
        }

        val cookie = requestHeaders["Cookie"]
        if (cookie != null) {
            val split = cookie.split("; ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (rawCookie in split) {
                val cook = rawCookie.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (cook.size == 2)
                    cookies[cook[0]] = cook[1]
            }
        }

        return DrillRequest(cookies["DrillSessionId"], requestHeaders["Host"])
    }

    @Throws(RuntimeException::class)
    private fun parseHeaderLine(header: String): Pair<String, String> {
        val idx = header.indexOf(":")
        if (idx == -1) {
            throw RuntimeException("Invalid Header Parameter: $header")
        }
        return header.substring(0, idx) to header.substring(idx + 1, header.length)
    }

}