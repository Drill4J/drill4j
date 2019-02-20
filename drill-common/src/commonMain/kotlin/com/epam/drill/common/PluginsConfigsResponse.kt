package com.epam.drill.common

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class PluginsConfigsResponse(val plugins: Map<String, String>? = null,val isExpanded: Boolean = true)

//    @Optional
//    var plugins: Map<String, PluginBean>? = null
//    var isExpanded: Boolean = true
//    var pane: Pane? = null
//    var icon: Icon? = null
//
//    inner class Pane {
//        var position: List<Int>? = null
//        var size: List<Int>? = null
//    }
//
//    inner class Icon {
//        var position: List<Int>? = null
//    }
////
//    init {
//        pane = Pane()
//        pane!!.position = arrayListOf(20, 20)
//        pane!!.size = arrayListOf(330, 270)
//
//        icon = Icon()
//        icon!!.position = arrayListOf(20, 20)
//    }
//}