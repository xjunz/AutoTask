package top.xjunz.tasker.engine.criterion

import top.xjunz.tasker.engine.valt.Distance

/**
 * @author xjunz 2022/09/27
 */
abstract class BoundsCriterion<T : Any> : Criterion<T, Distance>()

fun <T : Any> BoundsCriterion(bounds: (target: T, scope: Int, unit: Int, portionScope: Int) -> Float): BoundsCriterion<T> {
    return object : BoundsCriterion<T>() {
        override fun matchTarget(target: T, value: Distance): Boolean {
            if (value.rangeStart == Distance.NO_LIMIT && value.rangeEnd == Distance.NO_LIMIT) return true
            val distance = bounds(target, value.scope, value.unit, value.portionScope)
            return (value.rangeStart == Distance.NO_LIMIT || distance >= value.rangeStart)
                    && (value.rangeEnd == Distance.NO_LIMIT || distance <= value.rangeEnd)
        }
    }
}