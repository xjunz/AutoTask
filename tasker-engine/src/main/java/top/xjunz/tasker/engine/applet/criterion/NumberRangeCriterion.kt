/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.util.NumberRangeUtil

/**
 * @author xjunz 2022/08/14
 */
class NumberRangeCriterion<R : Any, T : Number>(
    rawType: Int = AppletValues.VAL_TYPE_INT,
    private inline val mapper: (R) -> T
) : Criterion<R, List<T>>() {

    override val valueType: Int = collectionTypeOf(rawType)

    override fun matchTarget(target: R, value: List<T>): Boolean {
        return NumberRangeUtil.contains(value[0], value[1]) {
            mapper(target)
        }
    }
}

inline fun <R : Any, reified T : Number> newNumberRangeCriterion(noinline mapper: (R) -> T)
        : NumberRangeCriterion<R, T> {
    return NumberRangeCriterion(AppletValues.judgeValueType<T>(), mapper)
}
