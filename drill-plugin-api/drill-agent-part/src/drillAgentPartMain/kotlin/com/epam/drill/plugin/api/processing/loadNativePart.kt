package com.epam.drill.plugin.api.processing

actual fun loadNativePart(nativePluginPartPath: String) {
    try {
        System.load(nativePluginPartPath)
    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}

actual fun prt(st: String) {
    System.out.println(st)
}