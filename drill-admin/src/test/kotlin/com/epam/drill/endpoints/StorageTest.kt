import com.epam.drill.common.AgentInfos
import com.epam.drill.common.Family
import com.epam.drill.common.PluginBeanDb
import com.epam.drill.common.PluginBeans
import com.epam.drill.dataclasses.JsonMessage
import com.epam.drill.dataclasses.JsonMessages
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
        val connect = Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
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
        }
        transaction(connect) {
            JsonMessage.new("xx") {
                message = "xxx"
            }
        }
        println()
        transaction(connect) {
            val map = JsonMessages.select {
                JsonMessages.id.eq("xx")
            }.map { it[JsonMessages.message] }.first()
            println(map)
        }
    }
}