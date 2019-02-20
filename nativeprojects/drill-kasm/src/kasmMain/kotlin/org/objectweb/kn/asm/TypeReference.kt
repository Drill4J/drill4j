/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2013 INRIA, France Telecom
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
 * A reference to a type appearing in a class, field or method declaration, or
 * on an instruction. Such a reference designates the part of the class where
 * the referenced type is appearing (e.g. an 'extends', 'implements' or 'throws'
 * clause, a 'new' instruction, a 'catch' clause, a type cast, a local variable
 * declaration, etc).
 *
 * @author Eric Bruneton
 */
class TypeReference
/**
 * Creates a new TypeReference.
 *
 * @param typeRef
 * the int encoded value of the type reference, as received in a
 * visit method related to type annotations, like
 * visitTypeAnnotation.
 */
(
        /**
         * The type reference value in Java class file format.
         */
        /**
         * Returns the int encoded value of this type reference, suitable for use in
         * visit methods related to type annotations, like visitTypeAnnotation.
         *
         * @return the int encoded value of this type reference.
         */
        val value: Int) {

    /**
     * Returns the sort of this type reference.
     *
     * @return [CLASS_TYPE_PARAMETER][.CLASS_TYPE_PARAMETER],
     * [METHOD_TYPE_PARAMETER][.METHOD_TYPE_PARAMETER],
     * [CLASS_EXTENDS][.CLASS_EXTENDS],
     * [CLASS_TYPE_PARAMETER_BOUND][.CLASS_TYPE_PARAMETER_BOUND],
     * [METHOD_TYPE_PARAMETER_BOUND][.METHOD_TYPE_PARAMETER_BOUND],
     * [FIELD][.FIELD], [METHOD_RETURN][.METHOD_RETURN],
     * [METHOD_RECEIVER][.METHOD_RECEIVER],
     * [METHOD_FORMAL_PARAMETER][.METHOD_FORMAL_PARAMETER],
     * [THROWS][.THROWS], [LOCAL_VARIABLE][.LOCAL_VARIABLE],
     * [RESOURCE_VARIABLE][.RESOURCE_VARIABLE],
     * [EXCEPTION_PARAMETER][.EXCEPTION_PARAMETER],
     * [INSTANCEOF][.INSTANCEOF], [NEW][.NEW],
     * [         CONSTRUCTOR_REFERENCE_RECEIVER][.CONSTRUCTOR_REFERENCE_RECEIVER],
     * [METHOD_REFERENCE_RECEIVER][.METHOD_REFERENCE_RECEIVER],
     * [CAST][.CAST], [         CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT][.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT],
     * [         METHOD_INVOCATION_TYPE_ARGUMENT][.METHOD_INVOCATION_TYPE_ARGUMENT],
     * [         CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT][.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT], or
     * [         METHOD_REFERENCE_TYPE_ARGUMENT][.METHOD_REFERENCE_TYPE_ARGUMENT].
     */
    val sort: Int
        get() = value.ushr(24)

    /**
     * Returns the index of the type parameter referenced by this type
     * reference. This method must only be used for type references whose sort
     * is [CLASS_TYPE_PARAMETER][.CLASS_TYPE_PARAMETER],
     * [METHOD_TYPE_PARAMETER][.METHOD_TYPE_PARAMETER],
     * [CLASS_TYPE_PARAMETER_BOUND][.CLASS_TYPE_PARAMETER_BOUND] or
     * [METHOD_TYPE_PARAMETER_BOUND][.METHOD_TYPE_PARAMETER_BOUND].
     *
     * @return a type parameter index.
     */
    val typeParameterIndex: Int
        get() = value and 0x00FF0000 shr 16

    /**
     * Returns the index of the type parameter bound, within the type parameter
     * [.getTypeParameterIndex], referenced by this type reference. This
     * method must only be used for type references whose sort is
     * [CLASS_TYPE_PARAMETER_BOUND][.CLASS_TYPE_PARAMETER_BOUND] or
     * [METHOD_TYPE_PARAMETER_BOUND][.METHOD_TYPE_PARAMETER_BOUND].
     *
     * @return a type parameter bound index.
     */
    val typeParameterBoundIndex: Int
        get() = value and 0x0000FF00 shr 8

    /**
     * Returns the index of the "super type" of a class that is referenced by
     * this type reference. This method must only be used for type references
     * whose sort is [CLASS_EXTENDS][.CLASS_EXTENDS].
     *
     * @return the index of an interface in the 'implements' clause of a class,
     * or -1 if this type reference references the type of the super
     * class.
     */
    val superTypeIndex: Int
        get() = (value and 0x00FFFF00 shr 8).toShort().toInt()

    /**
     * Returns the index of the formal parameter whose type is referenced by
     * this type reference. This method must only be used for type references
     * whose sort is [METHOD_FORMAL_PARAMETER][.METHOD_FORMAL_PARAMETER].
     *
     * @return a formal parameter index.
     */
    val formalParameterIndex: Int
        get() = value and 0x00FF0000 shr 16

    /**
     * Returns the index of the exception, in a 'throws' clause of a method,
     * whose type is referenced by this type reference. This method must only be
     * used for type references whose sort is [THROWS][.THROWS].
     *
     * @return the index of an exception in the 'throws' clause of a method.
     */
    val exceptionIndex: Int
        get() = value and 0x00FFFF00 shr 8

    /**
     * Returns the index of the try catch block (using the order in which they
     * are visited with visitTryCatchBlock), whose 'catch' type is referenced by
     * this type reference. This method must only be used for type references
     * whose sort is [EXCEPTION_PARAMETER][.EXCEPTION_PARAMETER] .
     *
     * @return the index of an exception in the 'throws' clause of a method.
     */
    val tryCatchBlockIndex: Int
        get() = value and 0x00FFFF00 shr 8

    /**
     * Returns the index of the type argument referenced by this type reference.
     * This method must only be used for type references whose sort is
     * [CAST][.CAST], [ CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT][.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT],
     * [METHOD_INVOCATION_TYPE_ARGUMENT][.METHOD_INVOCATION_TYPE_ARGUMENT],
     * [ CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT][.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT], or
     * [METHOD_REFERENCE_TYPE_ARGUMENT][.METHOD_REFERENCE_TYPE_ARGUMENT].
     *
     * @return a type parameter index.
     */
    val typeArgumentIndex: Int
        get() = value and 0xFF

    companion object {

        /**
         * The sort of type references that target a type parameter of a generic
         * class. See [getSort][.getSort].
         */
        val CLASS_TYPE_PARAMETER = 0x00

        /**
         * The sort of type references that target a type parameter of a generic
         * method. See [getSort][.getSort].
         */
        val METHOD_TYPE_PARAMETER = 0x01

        /**
         * The sort of type references that target the super class of a class or one
         * of the interfaces it implements. See [getSort][.getSort].
         */
        val CLASS_EXTENDS = 0x10

        /**
         * The sort of type references that target a bound of a type parameter of a
         * generic class. See [getSort][.getSort].
         */
        val CLASS_TYPE_PARAMETER_BOUND = 0x11

        /**
         * The sort of type references that target a bound of a type parameter of a
         * generic method. See [getSort][.getSort].
         */
        val METHOD_TYPE_PARAMETER_BOUND = 0x12

        /**
         * The sort of type references that target the type of a field. See
         * [getSort][.getSort].
         */
        val FIELD = 0x13

        /**
         * The sort of type references that target the return type of a method. See
         * [getSort][.getSort].
         */
        val METHOD_RETURN = 0x14

        /**
         * The sort of type references that target the receiver type of a method.
         * See [getSort][.getSort].
         */
        val METHOD_RECEIVER = 0x15

        /**
         * The sort of type references that target the type of a formal parameter of
         * a method. See [getSort][.getSort].
         */
        val METHOD_FORMAL_PARAMETER = 0x16

        /**
         * The sort of type references that target the type of an exception declared
         * in the throws clause of a method. See [getSort][.getSort].
         */
        val THROWS = 0x17

        /**
         * The sort of type references that target the type of a local variable in a
         * method. See [getSort][.getSort].
         */
        val LOCAL_VARIABLE = 0x40

        /**
         * The sort of type references that target the type of a resource variable
         * in a method. See [getSort][.getSort].
         */
        val RESOURCE_VARIABLE = 0x41

        /**
         * The sort of type references that target the type of the exception of a
         * 'catch' clause in a method. See [getSort][.getSort].
         */
        val EXCEPTION_PARAMETER = 0x42

        /**
         * The sort of type references that target the type declared in an
         * 'instanceof' instruction. See [getSort][.getSort].
         */
        val INSTANCEOF = 0x43

        /**
         * The sort of type references that target the type of the object created by
         * a 'new' instruction. See [getSort][.getSort].
         */
        val NEW = 0x44

        /**
         * The sort of type references that target the receiver type of a
         * constructor reference. See [getSort][.getSort].
         */
        val CONSTRUCTOR_REFERENCE_RECEIVER = 0x45

        /**
         * The sort of type references that target the receiver type of a method
         * reference. See [getSort][.getSort].
         */
        val METHOD_REFERENCE_RECEIVER = 0x46

        /**
         * The sort of type references that target the type declared in an explicit
         * or implicit cast instruction. See [getSort][.getSort].
         */
        val CAST = 0x47

        /**
         * The sort of type references that target a type parameter of a generic
         * constructor in a constructor call. See [getSort][.getSort].
         */
        val CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48

        /**
         * The sort of type references that target a type parameter of a generic
         * method in a method call. See [getSort][.getSort].
         */
        val METHOD_INVOCATION_TYPE_ARGUMENT = 0x49

        /**
         * The sort of type references that target a type parameter of a generic
         * constructor in a constructor reference. See [getSort][.getSort].
         */
        val CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A

        /**
         * The sort of type references that target a type parameter of a generic
         * method in a method reference. See [getSort][.getSort].
         */
        val METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B

        /**
         * Returns a type reference of the given sort.
         *
         * @param sort
         * [FIELD][.FIELD], [METHOD_RETURN][.METHOD_RETURN],
         * [METHOD_RECEIVER][.METHOD_RECEIVER],
         * [LOCAL_VARIABLE][.LOCAL_VARIABLE],
         * [RESOURCE_VARIABLE][.RESOURCE_VARIABLE],
         * [INSTANCEOF][.INSTANCEOF], [NEW][.NEW],
         * [            CONSTRUCTOR_REFERENCE_RECEIVER][.CONSTRUCTOR_REFERENCE_RECEIVER], or
         * [METHOD_REFERENCE_RECEIVER][.METHOD_REFERENCE_RECEIVER].
         * @return a type reference of the given sort.
         */
        fun newTypeReference(sort: Int): TypeReference {
            return TypeReference(sort shl 24)
        }

        /**
         * Returns a reference to a type parameter of a generic class or method.
         *
         * @param sort
         * [CLASS_TYPE_PARAMETER][.CLASS_TYPE_PARAMETER] or
         * [METHOD_TYPE_PARAMETER][.METHOD_TYPE_PARAMETER].
         * @param paramIndex
         * the type parameter index.
         * @return a reference to the given generic class or method type parameter.
         */
        fun newTypeParameterReference(sort: Int,
                                      paramIndex: Int): TypeReference {
            return TypeReference(sort shl 24 or (paramIndex shl 16))
        }

        /**
         * Returns a reference to a type parameter bound of a generic class or
         * method.
         *
         * @param sort
         * [CLASS_TYPE_PARAMETER][.CLASS_TYPE_PARAMETER] or
         * [METHOD_TYPE_PARAMETER][.METHOD_TYPE_PARAMETER].
         * @param paramIndex
         * the type parameter index.
         * @param boundIndex
         * the type bound index within the above type parameters.
         * @return a reference to the given generic class or method type parameter
         * bound.
         */
        fun newTypeParameterBoundReference(sort: Int,
                                           paramIndex: Int, boundIndex: Int): TypeReference {
            return TypeReference(
                sort shl 24 or (paramIndex shl 16)
                        or (boundIndex shl 8)
            )
        }

        /**
         * Returns a reference to the super class or to an interface of the
         * 'implements' clause of a class.
         *
         * @param itfIndex
         * the index of an interface in the 'implements' clause of a
         * class, or -1 to reference the super class of the class.
         * @return a reference to the given super type of a class.
         */
        fun newSuperTypeReference(itfIndex: Int): TypeReference {
            var itfIndex = itfIndex
            itfIndex = itfIndex and 0xFFFF
            return TypeReference(CLASS_EXTENDS shl 24 or (itfIndex shl 8))
        }

        /**
         * Returns a reference to the type of a formal parameter of a method.
         *
         * @param paramIndex
         * the formal parameter index.
         *
         * @return a reference to the type of the given method formal parameter.
         */
        fun newFormalParameterReference(paramIndex: Int): TypeReference {
            return TypeReference(METHOD_FORMAL_PARAMETER shl 24 or (paramIndex shl 16))
        }

        /**
         * Returns a reference to the type of an exception, in a 'throws' clause of
         * a method.
         *
         * @param exceptionIndex
         * the index of an exception in a 'throws' clause of a method.
         *
         * @return a reference to the type of the given exception.
         */
        fun newExceptionReference(exceptionIndex: Int): TypeReference {
            return TypeReference(THROWS shl 24 or (exceptionIndex shl 8))
        }

        /**
         * Returns a reference to the type of the exception declared in a 'catch'
         * clause of a method.
         *
         * @param tryCatchBlockIndex
         * the index of a try catch block (using the order in which they
         * are visited with visitTryCatchBlock).
         *
         * @return a reference to the type of the given exception.
         */
        fun newTryCatchReference(tryCatchBlockIndex: Int): TypeReference {
            return TypeReference(EXCEPTION_PARAMETER shl 24 or (tryCatchBlockIndex shl 8))
        }

        /**
         * Returns a reference to the type of a type argument in a constructor or
         * method call or reference.
         *
         * @param sort
         * [CAST][.CAST],
         * [            CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT][.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT],
         * [            METHOD_INVOCATION_TYPE_ARGUMENT][.METHOD_INVOCATION_TYPE_ARGUMENT],
         * [            CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT][.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT], or
         * [            METHOD_REFERENCE_TYPE_ARGUMENT][.METHOD_REFERENCE_TYPE_ARGUMENT].
         * @param argIndex
         * the type argument index.
         *
         * @return a reference to the type of the given type argument.
         */
        fun newTypeArgumentReference(sort: Int, argIndex: Int): TypeReference {
            return TypeReference(sort shl 24 or argIndex)
        }
    }
}
