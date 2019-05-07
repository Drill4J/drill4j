package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.core.analysis.IMethodCoverage
import org.jacoco.core.analysis.IPackageCoverage
import org.jacoco.core.internal.data.CRC64

data class CoverageKey(
    val id: String,
    val packageName: String? = null,
    val className: String? = null,
    val methodName: String? = null,
    val methodDesc: String? = null
) {
    override fun equals(other: Any?) = other is CoverageKey && id == other.id

    override fun hashCode() = id.hashCode()
}

val String.crc64: String get() = CRC64.classId(toByteArray()).toString(Character.MAX_RADIX)

val ICoverageNode.coverage: Double?
    get() {
        val ratio = this.instructionCounter.coveredRatio
        return if (ratio.isFinite()) ratio * 100.0 else null
    }

fun ICoverageNode.coverageKey(parent: ICoverageNode? = null): CoverageKey = when (this) {
    is IMethodCoverage -> CoverageKey(
        id = "${parent?.name}.${this.name}${this.desc}".crc64,
        packageName = parent?.name?.substringBeforeLast('/'),
        className = parent?.name,
        methodName = this.name,
        methodDesc = this.desc
    )
    is IClassCoverage -> CoverageKey(
        id = this.name.crc64,
        packageName = this.name.substringBeforeLast('/'),
        className = this.name
    )
    is IPackageCoverage -> CoverageKey(
        id = this.name.crc64,
        packageName = this.name
    )
    else -> CoverageKey(this.name.crc64)
}

fun CoverageKey.declaration(desc: String): String {
    val argsString = desc.substringAfter('(').replace(")", "")
    val declList = mutableListOf<String>()
    val args = if (argsString.endsWith(';')) argsString.substringBeforeLast(';').split(';')
    else argsString.split(';')

    args.forEach {arg ->
        when (arg) {
            "V" -> declList.add("void")
            "L" -> declList.add("long")
            "Z" -> declList.add("boolean")
            "I" -> declList.add("int")
            "F" -> declList.add("float")
            "B" -> declList.add("byte")
            "D" -> declList.add("double")
            "S" -> declList.add("short")
            "C" -> declList.add("char")
            else -> declList.add("${arg.substringAfterLast('/')}")
        }
    }
    var decl = declList.joinToString(separator = "; ")
    decl =
        if (decl.contains(";")) "(${decl.substringBeforeLast("; ")}) : ${decl.substringAfterLast("; ")}"
        else "() : $decl"
    println(decl)
    return decl
}
