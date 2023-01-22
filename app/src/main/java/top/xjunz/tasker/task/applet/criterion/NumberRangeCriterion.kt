/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.task.applet.util.NumberRangeUtil

/**
 * @author xjunz 2022/08/14
 */
class NumberRangeCriterion<R : Any, T : Number>(rawType: Int, private inline val mapper: (R) -> T) :
    Criterion<R, List<T>>() {

    override val valueType: Int = collectionTypeOf(rawType)

    override fun R.getActualValue(): Any {
        return mapper(this)
    }

    override fun matchTarget(target: R, value: List<T>): Boolean {
        return NumberRangeUtil.contains(value[0], value[1]) {
            mapper(target)
        }
    }
}

inline fun <R : Any, reified T : Number> numberRangeCriterion(noinline mapper: (R) -> T)
        : NumberRangeCriterion<R, T> {
    return NumberRangeCriterion(Applet.judgeValueType<T>(), mapper)
}

inline fun <reified T : Number> simpleNumberRangeCriterion(noinline mapper: (Unit) -> T)
        : NumberRangeCriterion<Unit, T> {
    return NumberRangeCriterion(Applet.judgeValueType<T>(), mapper)
}
