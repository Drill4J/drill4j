@file:Suppress("RemoveRedundantQualifierName")

package com.epam.drill.net

import kotlinx.cinterop.*
import platform.posix.*

class IP(val data: UByteArray) {

    val v0 get() = data[0]
    val v1 get() = data[1]
    val v2 get() = data[2]
    val v3 get() = data[3]
    val str get() = "$v0.$v1.$v2.$v3"
    val value: Int get() = (v0.toInt() shl 0) or (v1.toInt() shl 8) or (v2.toInt() shl 16) or (v3.toInt() shl 24)
    //val value: Int get() = (v0.toInt() shl 24) or (v1.toInt() shl 16) or (v2.toInt() shl 8) or (v3.toInt() shl 0)
    override fun toString(): String = str

    companion object {
        fun fromHost(host: String): IP {
            val hname = gethostbyname(host)
            val inetaddr = hname!!.pointed.h_addr_list!![0]!!
            return IP(
                ubyteArrayOf(
                    inetaddr[0].toUByte(),
                    inetaddr[1].toUByte(),
                    inetaddr[2].toUByte(),
                    inetaddr[3].toUByte()
                )
            )
        }
    }
}

fun CPointer<sockaddr_in>.set(ip: IP, port: Int) {
    val addr = this
    addr.pointed.sin_family = AF_INET.convert()
    addr.pointed.sin_addr.s_addr = ip.value.toUInt()
    addr.pointed.sin_port = swapBytes(port.toUShort())
}

fun resolveAddress(host: String, port: Int) = memScoped {

    val ip = IP.fromHost(host)
    val addr = allocArray<sockaddr_in>(1)
    addr.set(ip, port)
    println(ip)
    @Suppress("UNCHECKED_CAST")
    addr as CValuesRef<sockaddr>

}

fun swapBytes(v: UShort): UShort =
    (((v.toInt() and 0xFF) shl 8) or ((v.toInt() ushr 8) and 0xFF)).toUShort()


fun getAvailableBytes(sockRaw: ULong): Int {
    val bytes_available = intArrayOf(0, 0)
    ioctl(sockRaw.toInt(), FIONREAD, bytes_available.refTo(0))
    return bytes_available[0]

}

fun close(sockRaw: ULong) {
    platform.posix.shutdown(sockRaw.toInt(), SHUT_RDWR)
}

fun setSocketNonBlocking(sockRaw: ULong) {
    println(sockRaw)
    var flags = fcntl(sockRaw.toInt(), F_GETFL, 0)
    if (flags == -1) return
    flags = (flags or O_NONBLOCK)
    fcntl(sockRaw.toInt(), F_SETFL, flags)
}

fun checkErrors(name: String) {
    println("check the $name error")
}