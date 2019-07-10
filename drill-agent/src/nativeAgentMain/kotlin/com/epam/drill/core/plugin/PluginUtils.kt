package com.epam.drill.core.plugin

import com.epam.drill.core.*

fun pluginConfigById(pluginId: String) = exec { pl[pluginId]!! }

