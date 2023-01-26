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
    @StringRes private val substitutionRes: Int,
    valueType: Class<*>,
    val referenceType: Class<*>?,
    variantValueType: Int,
    private val isReference: Boolean?
) : ValueDescriptor(nameRes, valueType, variantValueType) {

    val substitution: CharSequence get() = if (substitutionRes == -1) name else substitutionRes.text

    val isReferenceOnly get() = isReference == true

    val isValueOnly get() = isReference == false

    val isTolerant get() = isReference == null
}