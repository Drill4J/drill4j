package com.epam.drill.session

object DrillRequest {

    external fun currentSession(): String?

    external operator fun get(key: String): String?
    fun RetransformClasses(classes: Array<Class<*>>) {
        RetransformClasses(classes.size, classes)

    }

    external fun RetransformClasses(count: Int, classes: Array<Class<*>>)
    external fun GetAllLoadedClasses(): Array<Class<*>>

}