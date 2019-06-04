package com.epam.drill.service

import com.epam.drill.common.ABVsConnectedTable
import com.epam.drill.common.AgentInfos
import com.epam.drill.common.APConnectedTable
import com.epam.drill.common.PluginBeans
import com.epam.drill.dataclasses.AgentBuildVersions
import com.epam.drill.dataclasses.JsonMessages
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger

class DataSourceRegistry {
    init {
        Database.connect(hikari())
        org.jetbrains.exposed.sql.transactions.transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PluginBeans, AgentInfos, APConnectedTable,ABVsConnectedTable, AgentBuildVersions, JsonMessages)
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = "jdbc:postgresql://localhost:5432/drill_base"
        config.username = "postgres"
        config.password = "password"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }


}

suspend fun <T> asyncTransaction(statement: suspend Transaction.() -> T) = withContext(Dispatchers.IO) {
    org.jetbrains.exposed.sql.transactions.experimental.transaction(statement = statement)
}

