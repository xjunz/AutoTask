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
    @StringRes
    private val nameRes: Int,
    val valueClass: Class<*>,
    val variantValueType: Int,
    val isCollection: Boolean,
) {

    val name: CharSequence get() = nameRes.text

    fun parseValueFromInput(str: String): Any? {
        if (valueClass == String::class.java)
            return str

        if (valueClass == Int::class.java || valueClass == Int::class.javaObjectType)
            return str.toIntOrNull()

        if (valueClass == Long::class.java || valueClass == Long::class.javaObjectType)
            return str.toLongOrNull()

        if (valueClass == Float::class.java || valueClass == Float::class.javaObjectType)
            return str.toFloatOrNull()

        return null
    }
}