package com.epam.drill.plugins.coverage

import org.jacoco.core.analysis.*
import org.jacoco.core.data.*
import org.jacoco.core.internal.data.*

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

val ICoverageNode.coverage get() = coverage(instructionCounter.totalCount)

val IBundleCoverage.javaClasses
    get() = packages
        .flatMap { it.classes }
        .map { cc ->
            cc.name to JavaClass(
                name = cc.name.substringAfterLast('/'),
                path = cc.name,
                methods = cc.methods.map {
                    JavaMethod(
                        ownerClass = cc.name,
                        name = it.name,
                        desc = it.desc
                    )
                }.toSet()

            )
        }.toMap()

val IBundleCoverage.totalsMap: Map<String, ICoverageNode>
    get() = packages.flatMap { p ->
        listOf(p.plainPair()) + p.classes.flatMap { c ->
            listOf(c.plainPair()) + c.methods.map { it.plainPair(c) }
        }

    }.toMap()

fun ICoverageNode.plainPair(parent: ICoverageNode? = null) = coverageKey(parent).id to plainCopy


fun ICoverageNode.coverage(total: Int) = when(total) {
    0 -> 0.0
    else -> instructionCounter.coveredCount * 100.0 / total
}

fun ExecutionDataStore.with(execData: Sequence<ExecClassData>): ExecutionDataStore {
    for (execDatum in execData) {
        put(ExecutionData(execDatum.id, execDatum.className, execDatum.probes.toBooleanArray()))
    }
    return this
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

fun IMethodCoverage.simpleMethodCoverage(ownerClass: String) = SimpleJavaMethodCoverage(
    name = name,
    desc = declaration(desc),
    coverage = coverage,
    ownerClass = ownerClass
)

/**
 * Converts ASM method description to declaration in java style with kotlin style of return type.
 *
 * Examples:
 * - ()V -> (): void
 * - (IZ)V -> (int, boolean): void
 * - (ILjava/lang/String;)V -> (int, String): void
 * - ([Ljava/lang/String;IJ)V -> (String[], int, long): void
 * - ([[IJLjava/lang/String;)Ljava/lang/String; -> (int[][], long, String): String
 * @param desc
 * ASM method description
 * @return declaration in java style with kotlin style of return type
 *
 */
fun declaration(desc: String): String {
    return """\((.*)\)(.+)""".toRegex().matchEntire(desc)?.run {
        val argDesc = groupValues[1]
        val returnTypeDesc = groupValues[2]
        val argTypes = parseDescTypes(argDesc).joinToString(separator = ", ")
        val returnType = parseDescTypes(returnTypeDesc).first()
        "($argTypes): $returnType"
    } ?: ""
}

fun parseDescTypes(argDesc: String): List<String> {
    val types = mutableListOf<String>()
    val descItr = argDesc.iterator()
    while (descItr.hasNext()) {
        val char = descItr.nextChar()
        val arg = parseDescType(char, descItr)
        types.add(arg)
    }
    return types
}

fun parseDescType(char: Char, charIterator: CharIterator): String = when(char) {
    'V' -> "void"
    'J' -> "long"
    'Z' -> "boolean"
    'I' -> "int"
    'F' -> "float"
    'B' -> "byte"
    'D' -> "double"
    'S' -> "short"
    'C' -> "char"
    '[' -> "${parseDescType(charIterator.nextChar(), charIterator)}[]"
    'L' -> {
        val objectDescSeq = charIterator.asSequence().takeWhile { it != ';' }
        val objectDesc = objectDescSeq.fold(StringBuilder()) { sBuilder, c -> sBuilder.append(c) }.toString()
        objectDesc.substringAfterLast("/")
    }
    else -> "!Error"
}
