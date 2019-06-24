package com.epam.drill.io

import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import kotlin.test.Test

class Wts {

    @Test
    fun test(){

        val borrow = IoBuffer.Pool.borrow()
        borrow.resetForWrite()
        borrow.writeFully("dd".toByteArray())

//        val build: ByteReadPacket = builder.build()
        println(borrow.capacity)
        println(borrow.canWrite())
        println("mm")
    }
}