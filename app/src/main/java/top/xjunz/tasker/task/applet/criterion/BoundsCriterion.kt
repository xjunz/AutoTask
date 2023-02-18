/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.task.applet.util.NumberRangeUtil
import top.xjunz.tasker.task.applet.value.Distance

/**
 * @author xjunz 2022/09/27
 */
class BoundsCriterion<T : Any>(
    val direction: Int,
    private inline val bounds: (target: T, scope: Int, unit: Int) -> Float
) : Criterion<T, Distance>() {

    override val valueType: Int = VAL_TYPE_LONG

    override fun matchTarget(target: T, value: Distance): Boolean {
        return NumberRangeUtil.contains(value.rangeStart, value.rangeEnd) {
            bounds(target, value.scope, value.unit)
        }
    }

    override fun serializeValueToString(value: Any): String {
        value as Distance
        return value.compose().toString(16)
    }

    override fun deserializeValueFromString(src: String): Any {
        return Distance.parse(src.toLong(16))
    }
}