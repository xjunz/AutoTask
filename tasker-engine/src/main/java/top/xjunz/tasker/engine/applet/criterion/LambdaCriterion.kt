/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.base.AppletResult

/**
 * @author xjunz 2023/01/23
 */
class LambdaCriterion<T : Any, V : Any>(
    override val valueType: Int,
    private inline val matcher: ((T, V) -> AppletResult)
) : Criterion<T, V>() {

    companion object {

        /**
         * Create a new criterion that maps its match target to the same type as the match value's
         * and simply compare whether they are equal.
         */
        inline fun <T : Any, reified V : Any> equalCriterion(noinline mapper: (T) -> V?)
                : Criterion<T, V> {
            return LambdaCriterion(judgeValueType<V>()) { t, v ->
                AppletResult.resultOf(mapper(t)) {
                    it == v
                }
            }
        }
    }

    override fun matchTarget(target: T, value: V): AppletResult {
        return matcher(target, value)
    }
}
