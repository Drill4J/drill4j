package com.epam.drill.plugins.coverage

import io.vavr.collection.*
import io.vavr.kotlin.*
import kotlinx.atomicfu.*
import org.jacoco.core.analysis.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.util.*
import java.io.*

typealias Methods = List<JavaMethod>

class MethodsComparator(
    private val bundle: IBundleCoverage? = null,
    private val result: IncrementalCache = IncrementalCache()
) {

    fun compareClasses(
        oldClasses: Map<String, Methods>,
        newClasses: Map<String, Methods>
    ): MethodChanges {
        val intersectedKeys = oldClasses.keys.intersect(newClasses.keys)
        result.subjoin(DiffType.DELETED, (oldClasses - intersectedKeys).values.flatten())
        result.subjoin(DiffType.NEW, (newClasses - intersectedKeys).values.flatten())
        intersectedKeys.forEach { className ->
            computeDiff(
                oldClasses[className] ?: listOf(),
                newClasses[className] ?: listOf()
            )
        }

        return result.filterMethods(bundle)
    }

    fun computeDiff(old: Methods, new: Methods) {
        val (oldAffected, newAffected) = filterUnaffected(old, new)
        var (oldFiltered, newFiltered) = filterUnique(oldAffected, newAffected)

        newFiltered.forEach { method ->
            val (diff, modified) = identifyDiffType(oldFiltered, method)
            if (diff != null) {
                oldFiltered -= modified!!
                result.subjoin(diff, listOf(method))
            } else {
                result.subjoin(DiffType.NEW, listOf(method))
            }
        }
        result.subjoin(DiffType.DELETED, oldFiltered)
    }

    fun filterUnaffected(old: Methods, new: Methods) = run {
        result.subjoin(DiffType.UNAFFECTED, new.intersect(old).toList())
        old - new to new - old
    }

    fun filterUnique(old: Methods, new: Methods) = run {
        val (oldUnique, oldMixed) = old.separateUnique(new)
        val (newUnique, newMixed) = new.separateUnique(oldMixed)
        result.subjoin(DiffType.NEW, newUnique)
        result.subjoin(DiffType.DELETED, oldUnique)
        oldMixed to newMixed
    }
}

fun identifyDiffType(
    list: Methods,
    method: JavaMethod
): Pair<DiffType?, JavaMethod?> {
    val name = list.find { method.nameIsModified(it) }
    val desc = list.find { method.descriptorIsModified(it) }
    val body = list.find { method.bodyIsModified(it) }
    return when {
        name != null -> DiffType.MODIFIED_NAME to name
        desc != null -> DiffType.MODIFIED_DESC to desc
        body != null -> DiffType.MODIFIED_BODY to body
        else -> null to null
    }
}

fun Methods.splitToFields() = Triple(map { it.name }, map { it.desc }, map { it.hash })

fun Methods.separateUnique(other: Methods) = run {
    val (names, descs, hashes) = other.splitToFields()
    val unique = this.filter { !(it.name in names || it.desc in descs || it.hash in hashes) }
    unique to this - unique
}

enum class DiffType {
    MODIFIED_NAME,
    MODIFIED_DESC,
    MODIFIED_BODY,
    NEW,
    DELETED,
    UNAFFECTED
}

class IncrementalCache {

    private val _map = atomic(LinkedHashMap.empty<DiffType, Methods>())

    val map get() = _map.value!!

    operator fun get(key: DiffType): Methods = map.getOrNull(key) ?: listOf()

    fun subjoin(key: DiffType, value: Methods) {
        _map.update { it.put(key, this[key] + value) }
    }

    fun filterMethods(bundle: IBundleCoverage?) =
        if (bundle != null) {
            bundle.toDataMap().run {
                map.mapValues { methods ->
                    methods.filter { this[it.ownerClass to it.sign] != null }
                }
            }
        } else {
            map
        }
            .toJavaMap()

}


class ASMClassParser(
    bytes: ByteArray,
    private val className: String
) {
    private val printer = Textifier()
    private val mp = TraceMethodVisitor(printer)
    private val node = ClassNode()

    init {
        ClassReader(bytes).accept(node, 0)
    }

    fun parseToJavaMethods() = node.methods
        .filter { !it.name.isNullOrEmpty() }
        .map { method ->
            JavaMethod(
                ownerClass = className,
                name = method.name,
                desc = method.desc,
                hash = method.bodyHash()
            )
        }

    fun MethodNode.bodyHash(): String {
        val inss = instructions.iterator().asSequence()
        val methodBody = inss.filter { it !is LabelNode && it !is LineNumberNode }.fold("") { acc, instr ->
            acc + instr.stringify()
        }
        return methodBody.crc64
    }

    private fun AbstractInsnNode.stringify(): String {
        accept(mp)
        val sw = StringWriter()
        printer.print(PrintWriter(sw))
        printer.getText().clear()
        return sw.toString()
    }
}
