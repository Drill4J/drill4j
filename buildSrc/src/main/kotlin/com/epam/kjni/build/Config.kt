package com.epam.kjni.build

import java.io.File

interface Config {
    val outputDir: File
    val systemClasses: List<String>


}

