package test.stubs

class CommonClass{

    fun pr() = "aaa"
}

@Suppress("UNUSED_PARAMETER", "UNUSED")
class Baseline {

    fun getjson(): String {
        CommonClass().pr()
        return "jsonValid"
    }

    fun abc(s: String) {
        println("privert")
    }

}

@Suppress("UNUSED_PARAMETER", "UNUSED")
class OneMethodAdded {

    fun aaaa(){
        println()
    }

    fun getjson(): String {
        CommonClass().pr()
        return "jsonValid"
    }

    fun abc(s: String) {
        println("privert")
    }

}

@Suppress("UNUSED_PARAMETER", "UNUSED")
class OneMethodModifiedOneDeleted {

    fun abc(s: String) {
        println("privert")
        val yy = 666
        print(yy)
    }

}
