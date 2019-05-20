package com.epam.drill.logger

object DLogger {

    operator fun invoke(name: String): Logger {
        return Logger()
    }


}