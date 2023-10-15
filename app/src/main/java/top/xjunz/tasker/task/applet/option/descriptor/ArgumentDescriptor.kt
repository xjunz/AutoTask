/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.descriptor

import top.xjunz.tasker.ktx.text

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
}