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
package org.objectweb.kn.asm

/**
 * A constant pool item. Constant pool items can be created with the 'newXXX'
 * methods in the [ClassWriter] class.
 *
 * @author Eric Bruneton
 */
internal class Item {

    /**
     * Index of this item in the constant pool.
     */
    var index: Int = 0

    /**
     * Type of this constant pool item. A single class is used to represent all
     * constant pool item types, in order to minimize the bytecode size of this
     * package. The value of this field is one of [ClassWriter.INT],
     * [ClassWriter.LONG], [ClassWriter.FLOAT],
     * [ClassWriter.DOUBLE], [ClassWriter.UTF8],
     * [ClassWriter.STR], [ClassWriter.CLASS],
     * [ClassWriter.NAME_TYPE], [ClassWriter.FIELD],
     * [ClassWriter.METH], [ClassWriter.IMETH],
     * [ClassWriter.MTYPE], [ClassWriter.INDY].
     *
     * MethodHandle constant 9 variations are stored using a range of 9 values
     * from [ClassWriter.HANDLE_BASE] + 1 to
     * [ClassWriter.HANDLE_BASE] + 9.
     *
     * Special Item types are used for Items that are stored in the ClassWriter
     * [ClassWriter.typeTable], instead of the constant pool, in order to
     * avoid clashes with normal constant pool items in the ClassWriter constant
     * pool's hash table. These special item types are
     * [ClassWriter.TYPE_NORMAL], [ClassWriter.TYPE_UNINIT] and
     * [ClassWriter.TYPE_MERGED].
     */
    var type: Int = 0

    /**
     * Value of this item, for an integer item.
     */
    var intVal: Int = 0

    /**
     * Value of this item, for a long item.
     */
    var longVal: Long = 0

    /**
     * First part of the value of this item, for items that do not hold a
     * primitive value.
     */
    var strVal1: String? = null

    /**
     * Second part of the value of this item, for items that do not hold a
     * primitive value.
     */
    var strVal2: String? = null

    /**
     * Third part of the value of this item, for items that do not hold a
     * primitive value.
     */
    var strVal3: String? = null

    /**
     * The hash code value of this constant pool item.
     */
    var hashCode: Int = 0

    /**
     * Link to another constant pool item, used for collision lists in the
     * constant pool's hash table.
     */
    var next: Item? = null

    /**
     * Constructs an uninitialized [Item].
     */
    constructor() {}

    /**
     * Constructs an uninitialized [Item] for constant pool element at
     * given position.
     *
     * @param index
     * index of the item to be constructed.
     */
    constructor(index: Int) {
        this.index = index
    }

    /**
     * Constructs a copy of the given item.
     *
     * @param index
     * index of the item to be constructed.
     * @param i
     * the item that must be copied into the item to be constructed.
     */
    constructor(index: Int, i: Item) {
        this.index = index
        type = i.type
        intVal = i.intVal
        longVal = i.longVal
        strVal1 = i.strVal1
        strVal2 = i.strVal2
        strVal3 = i.strVal3
        hashCode = i.hashCode
    }

    /**
     * Sets this item to an integer item.
     *
     * @param intVal
     * the value of this item.
     */
    fun set(intVal: Int) {
        this.type = ClassWriter.INT
        this.intVal = intVal
        this.hashCode = 0x7FFFFFFF and type + intVal
    }

    /**
     * Sets this item to a long item.
     *
     * @param longVal
     * the value of this item.
     */
    fun set(longVal: Long) {
        this.type = ClassWriter.LONG
        this.longVal = longVal
        this.hashCode = 0x7FFFFFFF and type + longVal.toInt()
    }

    /**
     * Sets this item to a float item.
     *
     * @param floatVal
     * the value of this item.
     */
    fun set(floatVal: Float) {
        this.type = ClassWriter.FLOAT
        this.intVal = floatVal.toBits()
        this.hashCode = 0x7FFFFFFF and type + floatVal.toInt()
    }

    /**
     * Sets this item to a double item.
     *
     * @param doubleVal
     * the value of this item.
     */
    fun set(doubleVal: Double) {
        this.type = ClassWriter.DOUBLE
        this.longVal = doubleVal.toRawBits()
        this.hashCode = 0x7FFFFFFF and type + doubleVal.toInt()
    }

    /**
     * Sets this item to an item that do not hold a primitive value.
     *
     * @param type
     * the type of this item.
     * @param strVal1
     * first part of the value of this item.
     * @param strVal2
     * second part of the value of this item.
     * @param strVal3
     * third part of the value of this item.
     */
    operator fun set(type: Int, strVal1: String?, strVal2: String?,
                     strVal3: String?) {
        this.type = type
        this.strVal1 = strVal1
        this.strVal2 = strVal2
        this.strVal3 = strVal3
        when (type) {
            ClassWriter.UTF8, ClassWriter.STR, ClassWriter.CLASS, ClassWriter.MTYPE, ClassWriter.TYPE_NORMAL -> {
                hashCode = 0x7FFFFFFF and type + strVal1.hashCode()
                return
            }
            ClassWriter.NAME_TYPE -> {
                hashCode = 0x7FFFFFFF and type + strVal1.hashCode() * strVal2.hashCode()
                return
            }
            // ClassWriter.FIELD:
            // ClassWriter.METH:
            // ClassWriter.IMETH:
            // ClassWriter.HANDLE_BASE + 1..9
            else -> hashCode = 0x7FFFFFFF and type + (strVal1.hashCode()
                    * strVal2.hashCode() * strVal3.hashCode())
        }
    }

    /**
     * Sets the item to an InvokeDynamic item.
     *
     * @param name
     * invokedynamic's name.
     * @param desc
     * invokedynamic's desc.
     * @param bsmIndex
     * zero based index into the class attribute BootrapMethods.
     */
    operator fun set(name: String, desc: String, bsmIndex: Int) {
        this.type = ClassWriter.INDY
        this.longVal = bsmIndex.toLong()
        this.strVal1 = name
        this.strVal2 = desc
        this.hashCode = 0x7FFFFFFF and ClassWriter.INDY + (bsmIndex
                * strVal1.hashCode() * strVal2.hashCode())
    }

    /**
     * Sets the item to a BootstrapMethod item.
     *
     * @param position
     * position in byte in the class attribute BootrapMethods.
     * @param hashCode
     * hashcode of the item. This hashcode is processed from the
     * hashcode of the bootstrap method and the hashcode of all
     * bootstrap arguments.
     */
    operator fun set(position: Int, hashCode: Int) {
        this.type = ClassWriter.BSM
        this.intVal = position
        this.hashCode = hashCode
    }

    /**
     * Indicates if the given item is equal to this one. *This method assumes
     * that the two items have the same [.type]*.
     *
     * @param i
     * the item to be compared to this one. Both items must have the
     * same [.type].
     * @return <tt>true</tt> if the given item if equal to this one,
     * <tt>false</tt> otherwise.
     */
    fun isEqualTo(i: Item): Boolean {
        when (type) {
            ClassWriter.UTF8, ClassWriter.STR, ClassWriter.CLASS, ClassWriter.MTYPE, ClassWriter.TYPE_NORMAL -> return i.strVal1 == strVal1
            ClassWriter.TYPE_MERGED, ClassWriter.LONG, ClassWriter.DOUBLE -> return i.longVal == longVal
            ClassWriter.INT, ClassWriter.FLOAT -> return i.intVal == intVal
            ClassWriter.TYPE_UNINIT -> return i.intVal == intVal && i.strVal1 == strVal1
            ClassWriter.NAME_TYPE -> return i.strVal1 == strVal1 && i.strVal2 == strVal2
            ClassWriter.INDY -> {
                return (i.longVal == longVal && i.strVal1 == strVal1
                        && i.strVal2 == strVal2)
            }
            // case ClassWriter.FIELD:
            // case ClassWriter.METH:
            // case ClassWriter.IMETH:
            // case ClassWriter.HANDLE_BASE + 1..9
            else -> return (i.strVal1 == strVal1 && i.strVal2 == strVal2
                    && i.strVal3 == strVal3)
        }
    }

}
