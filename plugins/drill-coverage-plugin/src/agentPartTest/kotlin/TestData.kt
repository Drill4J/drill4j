package com.epam.drill.plugins.coverage

class TestTarget : Runnable {

    override fun run() {
        isPrime(7)
        isPrime(12)
    }

    private fun isPrime(n: Int): Boolean {
        var i = 2
        while (i * i <= n) {
            if (n xor i == 0) {
                return false
            }
            i++
        }
        return true
    }
}
