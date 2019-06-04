/*import com.epam.drill.common.AgentInfos
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

class ExposeTest {
    @Test
    fun test() {
            var hik = hikari()
            val connect = Database.connect(hik)
            transaction(connect) {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(PluginBeans, AgentInfos, JsonMessages)
            val transaction = transaction {
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


//            val munich = AgentInfoDb.new("fcuk") {
//                name = "x"
//                groupName = "x"
//                description = "x"
//                isEnable = true
//                adminUrl = "x"
//                buildVersion = "x"
//
//            }
//            munich.rawPluginNames = SizedCollection(listOf(transaction))
            println()
        }
        val transaction1 = transaction(connect) {
            JsonMessage.new("xx") {
                message = "xxx"
            }
        }
        println()
        val transaction = transaction(connect) {
            val map = JsonMessages.select {
                JsonMessages.id.eq("xx")
            }.map { it[JsonMessages.message] }.first()


            println(map)
        }
//        transaction{
//            println(AgentInfoDb.all().iterator().next())
//        }
//        println(transaction.message)
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = "jdbc:postgresql://localhost:5432/drill_base"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.setUsername("postgres");
        config.setPassword("password");
        config.validate()
        return HikariDataSource(config)
    }

}*/
