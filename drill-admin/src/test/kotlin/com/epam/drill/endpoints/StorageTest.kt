import com.epam.drill.common.*
import com.epam.drill.dataclasses.*
import com.zaxxer.hikari.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.junit.*

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
