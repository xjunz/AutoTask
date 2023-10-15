/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.task.applet.util.NumberRangeUtil

/**
 * @author xjunz 2022/08/14
 */
class NumberRangeCriterion<R : Any, T : Number>(private inline val mapper: (R) -> T) :
    Criterion<R, List<T>>() {

    companion object {

        inline fun <R : Any, reified T : Number> numberRangeCriterion(noinline mapper: (R) -> T)
                : NumberRangeCriterion<R, T> {
            return NumberRangeCriterion(mapper)
        }

        inline fun <reified T : Number> simpleNumberRangeCriterion(noinline mapper: (Unit) -> T)
                : NumberRangeCriterion<Unit, T> {
            return NumberRangeCriterion(mapper)
        }

    }

    override fun matchTarget(target: R, value: List<T>): AppletResult {
        return AppletResult.resultOf(mapper(target)) {
            NumberRangeUtil.contains(value[0], value[1], it)
        }
    }
}
