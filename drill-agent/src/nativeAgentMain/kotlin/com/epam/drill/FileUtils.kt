package com.epam.drill

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.MemoryVfs
import com.soywiz.korio.file.std.openAsZip


suspend fun JarVfsFile.extractPluginFacilitiesTo(destination: VfsFile, filter: (VfsFile) -> Boolean = { true }) {
    val mem = MemoryVfs()
    this.openAsZip { pz -> pz.copyToTree(mem) }
    for (it in mem.list()) {
        if (filter(it))
            it.delete()
    }
    mem.copyToTree(destination)
}

typealias JarVfsFile = VfsFile