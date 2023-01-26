/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/09/22
 */
class PropertyCriterion<T : Any>(private inline val matcher: (target: T) -> Boolean) :
    Criterion<T, Boolean>() {

    override val valueType: Int = VAL_TYPE_IRRELEVANT

    override fun getDefaultValue(runtime: TaskRuntime): Boolean {
        return true
    }

    override fun matchTarget(target: T, value: Boolean): Boolean {
        return matcher(target) == value
    }
}