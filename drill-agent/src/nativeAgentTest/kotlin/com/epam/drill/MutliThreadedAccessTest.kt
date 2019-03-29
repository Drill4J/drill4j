package com.epam.drill

import com.epam.drill.core.di
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class MutliThreadedAccessTest {


    @Test
    fun accessWithoutCrash() = runBlocking {


    }
}


fun access() {
    runBlocking {
        initRuntimeIfNeeded()
        val get = di.x
        get?.send(byteArrayOf(1,2,3))
    }
}