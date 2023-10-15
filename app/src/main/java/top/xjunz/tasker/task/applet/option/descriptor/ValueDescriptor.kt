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
    val valueType: Class<*>,
    val variantType: Int,
    val isCollection: Boolean,
) {

    val isAnonymous: Boolean get() = nameRes == -1

    val name: CharSequence get() = nameRes.text

    fun parseValueFromInput(str: String): Any? {
        if (valueType == String::class.java)
            return str

        if (valueType == Int::class.java || valueType == Int::class.javaObjectType)
            return str.toIntOrNull()

        if (valueType == Long::class.java || valueType == Long::class.javaObjectType)
            return str.toLongOrNull()

        if (valueType == Float::class.java || valueType == Float::class.javaObjectType)
            return str.toFloatOrNull()

        return null
    }
}