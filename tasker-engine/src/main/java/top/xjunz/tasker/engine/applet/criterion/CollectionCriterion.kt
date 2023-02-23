/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.base.AppletResult

/**
 * @author xjunz 2022/08/14
 */
class CollectionCriterion<T : Any, V : Any>(
    rawType: Int,
    private inline val mapper: T.() -> V?
) : Criterion<T, List<V>>() {

    companion object {

        inline fun <T : Any, reified V : Any> collectionCriterion(noinline block: (T) -> V?)
                : CollectionCriterion<T, V> {
            return CollectionCriterion(judgeValueType<V>(), block)
        }
    }

    override val valueType: Int = collectionTypeOf(rawType)

    override fun matchTarget(target: T, value: List<V>): AppletResult {
        return AppletResult.resultOf(target.mapper()) {
            value.contains(it)
        }
    }
}