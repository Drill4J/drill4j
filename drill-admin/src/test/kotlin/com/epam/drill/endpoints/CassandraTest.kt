import com.epam.drill.common.AgentInfos
import com.epam.drill.common.Family
import com.epam.drill.common.PluginBeanDb
import com.epam.drill.common.PluginBeans
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test

class ExposeTest {
    @Test
    fun test() {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PluginBeans, AgentInfos)
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
//        transaction{
//            println(AgentInfoDb.all().iterator().next())
//        }
        println()
    }
}