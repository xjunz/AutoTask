/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.descriptor

import androidx.annotation.StringRes
import top.xjunz.tasker.ktx.text

/**
 * @author xjunz 2023/01/15
 */
class ArgumentDescriptor(
    nameRes: Int,
    private val substitutionRes: Int,
    valueClass: Class<*>,
    /**
     * Reference class may not be the same as value class
     */
    val referenceClass: Class<*>?,
    variantValueType: Int,
    private val isReference: Boolean?,
    isCollection: Boolean
) : ValueDescriptor(nameRes, valueClass, variantValueType, isCollection) {

    val substitution: CharSequence get() = if (substitutionRes == -1) name else substitutionRes.text

    val isReferenceOnly get() = isReference == true

    val isValueOnly get() = isReference == false

    val isTolerant get() = isReference == null

    class Builder(private val nameRes: Int, private val isReference: Boolean? = null) {

        lateinit var valueClass: Class<*>

        inline fun <reified T> ofType() {
            valueClass = T::class.java
        }

        @StringRes
        private var substitutionRes: Int = -1

        fun withSubstitution(@StringRes res: Int): Builder {
            substitutionRes = res
            return this
        }

        var variantValueType = -1

        fun withVariantValueType(variantValueType: Int): Builder {
            this.variantValueType = variantValueType
            return this
        }

        var referenceClass: Class<*>? = null

        fun withReferenceClass(referenceClass: Class<*>?): Builder {
            this.referenceClass = referenceClass
            return this
        }

        var isCollection = false

        fun asCollection() {
            isCollection = true
        }

        fun build(): ArgumentDescriptor {
            return ArgumentDescriptor(
                nameRes,
                substitutionRes,
                valueClass,
                referenceClass,
                variantValueType,
                isReference,
                isCollection
            )
        }
    }
}