import kotlin.reflect.*
import kotlin.test.*

fun asserThat(excepted: Any?, actual: KClass<*>, message: String? = "") {
    assertTrue(message) { actual.isInstance(excepted) }
}