import kotlin.reflect.KClass
import kotlin.test.assertTrue

fun asserThat(excepted: Any?, actual: KClass<*>, message: String? = "") {
    assertTrue(message) { actual.isInstance(excepted) }
}