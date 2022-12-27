/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.util.NumberRangeUtil
import top.xjunz.tasker.engine.value.Distance

/**
 * @author xjunz 2022/09/27
 */
class BoundsCriterion<T : Any>(
    val direction: Int,
    private inline val bounds: (target: T, scope: Int, unit: Int) -> Float
) : Criterion<T, Distance>() {

    override val valueType: Int = AppletValues.VAL_TYPE_DISTANCE

    override fun matchTarget(target: T, value: Distance): Boolean {
        return NumberRangeUtil.contains(value.rangeStart, value.rangeEnd) {
            bounds(target, value.scope, value.unit)
        }
    }
}