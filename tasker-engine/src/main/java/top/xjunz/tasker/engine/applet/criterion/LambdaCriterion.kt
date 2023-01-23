/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

/**
 * @author xjunz 2023/01/23
 */
class LambdaCriterion<T : Any, V : Any>(
    override val valueType: Int,
    private inline val matcher: ((T, V) -> Boolean)
) : Criterion<T, V>() {

    companion object {

        inline fun <T : Any, reified V : Any> newCriterion(noinline matcher: ((T, V) -> Boolean))
                : Criterion<T, V> {
            return LambdaCriterion(judgeValueType<V>(), matcher)
        }
    }

    override fun matchTarget(target: T, value: V): Boolean {
        return matcher(target, value)
    }
}
