package com.epam.drill

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BuildVersionCalculator {

    @Test
    fun parallelCalculation() = runBlocking<Unit>(Dispatchers.IO) {

    }
}