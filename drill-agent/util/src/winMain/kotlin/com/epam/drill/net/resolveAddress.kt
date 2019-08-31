package com.epam.drill.net

import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.LPADDRINFOVar

fun resolveAddress(host: String, port: Int) = memScoped {
    val addr = allocArray<LPADDRINFOVar>(1)
    val alloc = alloc<platform.windows.addrinfo>()
    alloc.ai_family = AF_INET
    alloc.ai_socktype = SOCK_STREAM
    alloc.ai_protocol = IPPROTO_TCP
    platform.windows.getaddrinfo(host, port.toString(), alloc.ptr, addr)
    val info = addr[0]!!.pointed
    val aiAddr: CPointer<sockaddr> = info.ai_addr!!
    aiAddr as CValuesRef<sockaddr>
}

fun getAvailableBytes(sockRaw: ULong): Int {
    val bytes_available = intArrayOf(0, 0)
    @Suppress("UNCHECKED_CAST")
    ioctlsocket(
        sockRaw,
        FIONREAD,
        bytes_available.refTo(0) as CValuesRef<u_longVar>
    )
    return bytes_available[0]

}

fun close(sockRaw: ULong) {
    closesocket(sockRaw)
}

fun setSocketNonBlocking(sockRaw: ULong) {
    memScoped {
        val mode = alloc<u_longVar>()
        mode.value = 1.convert()
        (ioctlsocket(sockRaw, FIONBIO.convert(), mode.ptr) == 0)
    }
}

fun checkErrors(name: String) {
    val error = platform.windows.WSAGetLastError()
    if (error != 0) {
        error("WSA error($name): $error")
    }
}