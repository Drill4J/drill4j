package com.epam.kjni.build

import com.epam.kjni.build.core.generateJavaPrimitivesWrapper
import com.epam.kjni.build.core.systemClasses
import java.io.File

fun generate(files: List<String>?, outputDir: File) {
//    generateJavaPrimitivesWrapper(outputDir)
    systemClasses(outputDir)
    //    val classParser =
    //        ClassParser("build/classes/kotlin/jvm/main/com/epam/kjni/testdata/IdealClass.class")
    //    val metaOfTheClass = classParser.parse()
    //    NativeGenerator().generateSingleClassByMeta(metaOfTheClass,outputDir)

}