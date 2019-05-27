package com.epam.drill.core.plugin

import com.epam.drill.core.exec

fun pluginConfigById(pluginId: String) = exec { pl[pluginId]!! }

