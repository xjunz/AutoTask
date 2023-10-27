/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.descriptor

import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.value.VariantArgType

/**
 * @author xjunz 2023/01/15
 */
class ArgumentDescriptor(
    nameRes: Int,
    private val substitutionRes: Int,
    valueType: Class<*>,
    /**
     * Reference class may not be the same as value class
     */
    val referenceType: Class<*>?,
    variantValueType: Int,
    private val isReference: Boolean?,
    isCollection: Boolean
) : ValueDescriptor(nameRes, valueType, variantValueType, isCollection) {

    val substitution: CharSequence get() = if (substitutionRes == -1) name else substitutionRes.text

    val isReferenceOnly get() = isReference == true

    val isValueOnly get() = isReference == false

    val isTolerant get() = isReference == null

    /**
     * Covert to a [ValueDescriptor]. If [referenceType] is not null, the result's value type
     * will be the [referenceType] and the [variantValueType] will be cleared, because a result is
     * to be referred.
     */
    fun asResult(): ValueDescriptor {
        if (referenceType != null) {
            return ValueDescriptor(nameRes, referenceType, VariantArgType.NONE, isCollection)
        }
        return this
    }
}