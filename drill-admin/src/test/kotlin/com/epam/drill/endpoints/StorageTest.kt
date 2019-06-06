import com.epam.drill.common.AgentInfos
import com.epam.drill.common.Family
import com.epam.drill.common.PluginBeanDb
import com.epam.drill.common.PluginBeans
import com.epam.drill.dataclasses.JsonMessage
import com.epam.drill.dataclasses.JsonMessages
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test

class StorageTest {
    @Test
    fun test() {
        val hik = hikari()
        val connect = Database.connect(hik)
        transaction(connect) {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(PluginBeans, AgentInfos, JsonMessages)
            transaction {
                val pb = PluginBeanDb.new {
                    pluginId = "x"
                    name = "x"
                    description = "x"
                    type = "x"
                    family = Family.GENERIC
                    enabled = true
                    config = "x"
                }
                pb
            }

            println()
        }
        transaction(connect) {
            JsonMessage.new("xx") {
                message = "xxx"
            }
        }

        transaction(connect) {
            val map = JsonMessages.select {
                JsonMessages.id.eq("xx")
            }.map { it[JsonMessages.message] }.first()


            println(map)
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        return HikariDataSource(config)
    }

}
