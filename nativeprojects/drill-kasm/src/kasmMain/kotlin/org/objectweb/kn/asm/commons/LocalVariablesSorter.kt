/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.kn.asm.commons

import org.objectweb.kn.asm.*
import kotlin.math.max

/**
 * A [MethodVisitor] that renumbers local variables in their order of
 * appearance. This adapter allows one to easily add new local variables to a
 * method. It may be used by inheriting from this class, but the preferred way
 * of using it is via delegation: the next visitor in the chain can indeed add
 * new locals when needed by calling [.newLocal] on this adapter (this
 * requires a reference back to this [LocalVariablesSorter]).
 *
 * @author Chris Nokleberg
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
open class LocalVariablesSorter
/**
 * Creates a new [LocalVariablesSorter].
 *
 * @param api
 * the ASM API version implemented by this visitor. Must be one
 * of [Opcodes.ASM4] or [Opcodes.ASM5].
 * @param access
 * access flags of the adapted method.
 * @param desc
 * the method's descriptor (see [Type]).
 * @param mv
 * the method visitor to which this adapter delegates calls.
 */
protected constructor(
    api: Int, access: Int,
    desc: String, mv: MethodVisitor
) : MethodVisitor(api, mv) {

    /**
     * Mapping from old to new local variable indexes. A local variable at index
     * i of size 1 is remapped to 'mapping[2*i]', while a local variable at
     * index i of size 2 is remapped to 'mapping[2*i+1]'.
     */
    private var mapping = IntArray(40)

    /**
     * Array used to store stack map local variable types after remapping.
     */
    private var newLocals = arrayOfNulls<Any>(20)

    /**
     * Index of the first local variable, after formal parameters.
     */
    protected val firstLocal: Int

    /**
     * Index of the next local variable to be created by [.newLocal].
     */
    protected var nextLocal: Int = 0

    /**
     * Indicates if at least one local variable has moved due to remapping.
     */
    private var changed: Boolean = false

    /**
     * Creates a new [LocalVariablesSorter]. *Subclasses must not use
     * this constructor*. Instead, they must use the
     * [.LocalVariablesSorter] version.
     *
     * @param access
     * access flags of the adapted method.
     * @param desc
     * the method's descriptor (see [Type]).
     * @param mv
     * the method visitor to which this adapter delegates calls.
     */
    constructor(
        access: Int, desc: String,
        mv: MethodVisitor
    ) : this(Opcodes.ASM5, access, desc, mv) {
    }

    init {
        val args = Type.getArgumentTypes(desc)
        nextLocal = if (Opcodes.ACC_STATIC and access == 0) 1 else 0
        for (i in args.indices) {
            nextLocal += args[i].size
        }
        firstLocal = nextLocal
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        val type: Type
        when (opcode) {
            Opcodes.LLOAD, Opcodes.LSTORE -> type = Type.LONG_TYPE

            Opcodes.DLOAD, Opcodes.DSTORE -> type = Type.DOUBLE_TYPE

            Opcodes.FLOAD, Opcodes.FSTORE -> type = Type.FLOAT_TYPE

            Opcodes.ILOAD, Opcodes.ISTORE -> type = Type.INT_TYPE

            else ->
                // case Opcodes.ALOAD:
                // case Opcodes.ASTORE:
                // case RET:
                type = OBJECT_TYPE
        }
        mv!!.visitVarInsn(opcode, remap(`var`, type))
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        mv!!.visitIincInsn(remap(`var`, Type.INT_TYPE), increment)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        mv!!.visitMaxs(maxStack, nextLocal)
    }

    override fun visitLocalVariable(
        name: String, desc: String, signature: String?,
        start: Label, end: Label, index: Int
    ) {
        val newIndex = remap(index, Type.getType(desc))
        mv!!.visitLocalVariable(name, desc, signature, start, end, newIndex)
    }

    override fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath, start: Array<Label?>, end: Array<Label?>, index: IntArray,
        desc: String, visible: Boolean
    ): AnnotationVisitor? {
        val t = Type.getType(desc)
        val newIndex = IntArray(index.size)
        for (i in newIndex.indices) {
            newIndex[i] = remap(index[i], t)
        }
        return mv!!.visitLocalVariableAnnotation(
            typeRef, typePath, start, end,
            newIndex, desc, visible
        )
    }

    override fun visitFrame(
        type: Int, nLocal: Int, local: Array<Any?>, nStack: Int,
        stack: Array<Any?>
    ) {
        if (type != Opcodes.F_NEW) { // uncompressed frame
            throw IllegalStateException(
                "ClassReader.accept() should be called with EXPAND_FRAMES flag"
            )
        }

        if (!changed) { // optimization for the case where mapping = identity
            mv!!.visitFrame(type, nLocal, local, nStack, stack)
            return
        }

        // creates a copy of newLocals
        val oldLocals = arrayOfNulls<Any>(newLocals.size)
        newLocals.copyInto(oldLocals,0,0, oldLocals.size)
//        System.arraycopy(newLocals, 0, oldLocals, 0, oldLocals.size)

        updateNewLocals(newLocals)

        // copies types from 'local' to 'newLocals'
        // 'newLocals' already contains the variables added with 'newLocal'

        var index = 0 // old local variable index
        var number = 0 // old local variable number
        while (number < nLocal) {
            val t = local[number]
            val size = if (t === Opcodes.LONG || t === Opcodes.DOUBLE) 2 else 1
            if (t !== Opcodes.TOP) {
                var typ = OBJECT_TYPE
                if (t === Opcodes.INTEGER) {
                    typ = Type.INT_TYPE
                } else if (t === Opcodes.FLOAT) {
                    typ = Type.FLOAT_TYPE
                } else if (t === Opcodes.LONG) {
                    typ = Type.LONG_TYPE
                } else if (t === Opcodes.DOUBLE) {
                    typ = Type.DOUBLE_TYPE
                } else if (t is String) {
                    typ = Type.getObjectType(t)
                }
                setFrameLocal(remap(index, typ), t!!)
            }
            index += size
            ++number
        }

        // removes TOP after long and double types as well as trailing TOPs

        index = 0
        number = 0
        var i = 0
        while (index < newLocals.size) {
            val t = newLocals[index++]
            if (t != null && t !== Opcodes.TOP) {
                newLocals[i] = t
                number = i + 1
                if (t === Opcodes.LONG || t === Opcodes.DOUBLE) {
                    index += 1
                }
            } else {
                newLocals[i] = Opcodes.TOP
            }
            ++i
        }

        // visits remapped frame
        mv!!.visitFrame(type, number, newLocals, nStack, stack)

        // restores original value of 'newLocals'
        newLocals = oldLocals
    }

    // -------------

    /**
     * Creates a new local variable of the given type.
     *
     * @param type
     * the type of the local variable to be created.
     * @return the identifier of the newly created local variable.
     */
    fun newLocal(type: Type): Int {
        val t: Any
        when (type.sort) {
            Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> t = Opcodes.INTEGER
            Type.FLOAT -> t = Opcodes.FLOAT
            Type.LONG -> t = Opcodes.LONG
            Type.DOUBLE -> t = Opcodes.DOUBLE
            Type.ARRAY -> t = type.descriptor
            // case Type.OBJECT:
            else -> t = type.internalName
        }
        val local = newLocalMapping(type)
        setLocalType(local, type)
        setFrameLocal(local, t)
        return local
    }

    /**
     * Notifies subclasses that a new stack map frame is being visited. The
     * array argument contains the stack map frame types corresponding to the
     * local variables added with [.newLocal]. This method can update
     * these types in place for the stack map frame being visited. The default
     * implementation of this method does nothing, i.e. a local variable added
     * with [.newLocal] will have the same type in all stack map frames.
     * But this behavior is not always the desired one, for instance if a local
     * variable is added in the middle of a try/catch block: the frame for the
     * exception handler should have a TOP type for this new local.
     *
     * @param newLocals
     * the stack map frame types corresponding to the local variables
     * added with [.newLocal] (and null for the others). The
     * format of this array is the same as in
     * [MethodVisitor.visitFrame], except that long and double
     * types use two slots. The types for the current stack map frame
     * must be updated in place in this array.
     */
    protected fun updateNewLocals(newLocals: Array<Any?>) {}

    /**
     * Notifies subclasses that a local variable has been added or remapped. The
     * default implementation of this method does nothing.
     *
     * @param local
     * a local variable identifier, as returned by [            newLocal()][.newLocal].
     * @param type
     * the type of the value being stored in the local variable.
     */
    protected open fun setLocalType(local: Int, type: Type) {}

    private fun setFrameLocal(local: Int, type: Any) {
        val l = newLocals.size
        if (local >= l) {
            val a = arrayOfNulls<Any>(max(2 * l, local + 1))
            newLocals.copyInto(a,0,0,1)
//            System.arraycopy(newLocals, 0, a, 0, l)
            newLocals = a
        }
        newLocals[local] = type
    }

    private fun remap(`var`: Int, type: Type): Int {
        if (`var` + type.size <= firstLocal) {
            return `var`
        }
        val key = 2 * `var` + type.size - 1
        val size = mapping.size
        if (key >= size) {
            val newMapping = IntArray(max(2 * size, key + 1))
            mapping.copyInto(newMapping,0,0,1)
//            System.arraycopy(mapping, 0, newMapping, 0, size)
            mapping = newMapping
        }
        var value = mapping[key]
        if (value == 0) {
            value = newLocalMapping(type)
            setLocalType(value, type)
            mapping[key] = value + 1
        } else {
            value--
        }
        if (value != `var`) {
            changed = true
        }
        return value
    }

    protected fun newLocalMapping(type: Type): Int {
        val local = nextLocal
        nextLocal += type.size
        return local
    }

    companion object {

        private val OBJECT_TYPE = Type
            .getObjectType("java/lang/Object")
    }
}
