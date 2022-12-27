/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option

import androidx.annotation.StringRes
import top.xjunz.tasker.ktx.text

/**
 * @author xjunz 2022/11/20
 */
class ValueDescriptor(
    @StringRes private val nameRes: Int,
    val type: Class<*>,
    private val isReference: Boolean?
) {

    val name: CharSequence get() = nameRes.text

    val isReferenceOnly get() = isReference == true

    val isValueOnly get() = isReference == false

    val isTolerant get() = isReference == null

    fun parseValueFromInput(str: String): Any? {
        if (type == String::class.java)
            return str

        if (type == Int::class.java)
            return str.toIntOrNull()

        if (type == Long::class.java)
            return str.toLongOrNull()

        if (type == Float::class.java)
            return str.toFloatOrNull()

        return null
    }

    override fun toString(): String {
        return "ValueDescriptor(type=$type, isReference=$isReference, name=$name)"
    }


}