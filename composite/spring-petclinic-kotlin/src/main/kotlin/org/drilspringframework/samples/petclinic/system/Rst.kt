package org.drilspringframework.samples.petclinic.system

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Rst {


    @RequestMapping("/getJson")
    fun getjson(): String {
        abc("""ss""");
//        someprocessin
        return "jsonValid"
    }

    public fun abc(s: String) {
        println("privert")

    }
}
