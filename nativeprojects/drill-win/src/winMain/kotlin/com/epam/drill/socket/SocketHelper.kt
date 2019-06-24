package com.epam.drill.socket

import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import platform.posix.AF_INET
import platform.posix.IPPROTO_TCP
import platform.posix.SOCK_STREAM
import platform.windows.LPADDRINFOVar

fun resolveAddress(host: String, port: Int) = memScoped {
    val addr = allocArray<LPADDRINFOVar>(1)
    val alloc = alloc<platform.windows.addrinfo>()
    alloc.ai_family = AF_INET
    alloc.ai_socktype = SOCK_STREAM
    alloc.ai_protocol = IPPROTO_TCP
    val res = platform.windows.getaddrinfo(host, port.toString(), alloc.ptr, addr)
    val info = addr[0]!!.pointed
    info.ai_addr!!
}