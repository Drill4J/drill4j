package com.epam.drill.service

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.dataclasses.*
import com.zaxxer.hikari.*
import kotlinx.coroutines.*
import mu.*
import org.jetbrains.exposed.sql.*
import java.lang.System.*

private val logger = KotlinLogging.logger {}

class DataSourceRegistry {
    init {
        Database.connect(hikari())
        org.jetbrains.exposed.sql.transactions.transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(
                PluginBeans,
                AgentInfos,
                APConnectedTable,
                ABVsConnectedTable,
                AgentBuildVersions,
                JsonMessages
            )
        }
    }
}

private fun hikari() = HikariDataSource(
    HikariConfig().apply {
        when (getenv("DB_DRIVER")) {
            "postgresql" -> {
                driverClassName = "org.postgresql.Driver"
                val host = getenv("POSTGRES_HOST") ?: "localhost"
                val port = (getenv("POSTGRES_PORT") ?: "5432")
                jdbcUrl = "jdbc:postgresql://$host:$port/drill_base"
                username = "postgres"
                password = "password"

            }
            else -> { // embedded h2 db by default
                val dbPath = drillWorkDir.resolve("data").resolve("settings").toURI().path
                logger.info { "External db is not configured. Backing to embedded H2 database $dbPath." }
                driverClassName = "org.h2.Driver"
                jdbcUrl = "jdbc:h2:$dbPath"
            }
        }
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }.apply(HikariConfig::validate)
)

suspend fun <T> asyncTransaction(statement: suspend Transaction.() -> T) = withContext(Dispatchers.IO) {
    org.jetbrains.exposed.sql.transactions.experimental.transaction(statement = statement)
}

