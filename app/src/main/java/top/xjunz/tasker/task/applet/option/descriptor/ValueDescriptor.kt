/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.descriptor

import androidx.annotation.StringRes
import top.xjunz.tasker.ktx.text

/**
 * @author xjunz 2022/11/20
 */
open class ValueDescriptor(
    @StringRes private val nameRes: Int,
    val valueType: Class<*>,
    val variantValueType: Int
) {

    val name: CharSequence get() = nameRes.text

    fun parseValueFromInput(str: String): Any? {
        if (valueType == String::class.java)
            return str

        if (valueType == Int::class.java)
            return str.toIntOrNull()

        if (valueType == Long::class.java)
            return str.toLongOrNull()

        if (valueType == Float::class.java)
            return str.toFloatOrNull()

        return null
    }

}