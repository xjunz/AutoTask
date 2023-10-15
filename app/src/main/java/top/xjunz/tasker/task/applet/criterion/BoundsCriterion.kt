/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.base.AppletResult
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

    override fun matchTarget(target: T, value: Distance): AppletResult {
        return AppletResult.resultOf(bounds(target, value.scope, value.unit)) {
            NumberRangeUtil.contains(value.rangeStart, value.rangeEnd, it)
        }
    }

    override fun serializeArgumentToString(which: Int, rawType: Int, arg: Any): String {
        arg as Distance
        return arg.compose().toString(16)
    }

    override fun deserializeArgumentFromString(which: Int, rawType: Int, src: String): Any {
        return Distance.parse(src.toLong(16))
    }
}