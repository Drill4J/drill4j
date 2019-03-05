package com.epam.drill

import com.epam.drill.core.ws.readBinary
import com.soywiz.korio.file.std.MemoryVfs
import com.soywiz.korio.file.writeToFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BinaryEventParser {


    @Test
    fun readInstructions() = runBlocking {


        val rawData = byteArrayOf(
            0,
            0,
            0,
            11,
            0,
            0,
            0,
            14,
            115,
            111,
            109,
            101,
            77,
            101,
            115,
            115,
            97,
            103,
            101,
            112,
            114,
            105,
            118,
            101,
            116,
            102,
            114,
            111,
            109,
            102,
            105,
            108,
            101
        )

        val readBinary = readBinary(rawData)
        val stringFromUtf8 = readBinary.first.stringFromUtf8()
        assertEquals("someMessage", stringFromUtf8)
        val file = MemoryVfs()
        readBinary.second.writeToFile(file)
        assertEquals("privetfromfile", file.readString())

    }


}