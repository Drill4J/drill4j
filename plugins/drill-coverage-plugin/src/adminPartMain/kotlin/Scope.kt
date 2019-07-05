package com.epam.drill.plugins.coverage

import java.util.*


typealias Scopes = Set<String>

data class Scope(
    val name: String = "",
    val probes: MutableList<ExDataTemp> = mutableListOf(),
    var enabled: Boolean = true,
    var startDate: Date = Date(),
    var finishDate: Date? = null,
    var duration: Long = 0,
    var sessions: Int = 0
){
    fun start(){
        startDate = Date()
        finishDate = null
    }

    fun stop(){
        finishDate = Date()
        duration += finishDate!!.time - startDate.time
    }
}



data class ScopesKey(
    val buildVersion: String
) : StoreKey<Scopes>

data class ScopeKey(
    val buildVersion: String,
    val name: String = ""
) : StoreKey<Scope>