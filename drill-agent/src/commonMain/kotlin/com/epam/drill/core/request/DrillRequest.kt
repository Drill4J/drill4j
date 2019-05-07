package com.epam.drill.core.request

data class DrillRequest(
    val drillSessionId: String?,
    val host: String?,
    val additionalConfig: String?,
    val headers: Map<String, String>
) {
    fun get(key: String?): String? = headers[key]
}

typealias RawHttpRequest = String

const val COOKIE_HEADER_NAME = "Cookie"

fun parseHttpRequest(request: RawHttpRequest): HttpRequest {
    val reader = request.lineSequence()
    val query = reader.first().toRequestQuery()
    val requestHeaders = parseHeaders(reader.drop(1))
    val cookies = parseCookies(requestHeaders)
    return HttpRequest(query, requestHeaders, cookies)

}

private fun parseCookies(requestHeaders: Map<String, String>) = requestHeaders[COOKIE_HEADER_NAME]?.run {
    val cookies = mutableMapOf<String, String>()
    val split = this.trimStart().split("; ").dropLastWhile { it.isBlank() }
    for (rawCookie in split) {
        val eqIndex = rawCookie.indexOfFirst { it == '=' }
        val key = rawCookie.slice(0 until eqIndex)
        val value = rawCookie.slice(eqIndex + 1 until rawCookie.length)
        cookies[key] = value
    }
    cookies
} ?: mutableMapOf()


private fun parseHeaders(rawHeaders: Sequence<String>) = rawHeaders.filter { it.isNotBlank() }.associate { parseHeaderLine(it) }

private fun parseHeaderLine(header: String): Pair<String, String> {
    val idx = header.indexOfFirst { it == ':' }
    if (idx == -1) {
        throw RuntimeException("Invalid Header Parameter: $header")
    }
    return header.substring(0, idx) to header.substring(idx + 2, header.length)
}


data class HttpRequest(
    val query: Query,
    val headers: Map<String, String>,
    val cookies: Map<String, String>
)

fun HttpRequest.toRawRequestString() =
    "$query\n" +
            "${headers.map { it.key + ": " + it.value }.joinToString("\n")}\n" +
            "$COOKIE_HEADER_NAME:  ${cookies.map { it.key + "=" + it.value }.joinToString("; ")}"

fun HttpRequest.toDrillRequest(): DrillRequest {
    val optimizedHeaders = this.headers.mapKeys { it.key.toLowerCase() }
    return DrillRequest(
        optimizedHeaders["drill-session-id"] ?: this.cookies["DrillSessionId"],
        this.headers["host"],
        this.headers["drill-additional-config"],
        optimizedHeaders
    )
}

fun RawHttpRequest.toRequestQuery(): Query {
    val queryValues = this.split(" ")
    return Query(queryValues[0], queryValues[1], queryValues[2])
}

data class Query(val method: String, val url: String, val version: String) {
    override fun toString(): String {
        return "$method $url $version"
    }
}