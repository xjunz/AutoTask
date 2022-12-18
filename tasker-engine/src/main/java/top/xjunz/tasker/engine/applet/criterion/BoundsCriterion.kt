package top.xjunz.tasker.engine.applet.criterion

import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.util.NumberRangeUtil
import top.xjunz.tasker.engine.value.Distance

/**
 * @author xjunz 2022/09/27
 */
abstract class BoundsCriterion<T : Any>(@Transient val direction: Int) : Criterion<T, Distance>() {

    override val valueType: Int = AppletValues.VAL_TYPE_DISTANCE

}

fun <T : Any> boundsCriterion(
    direction: Int,
    bounds: (target: T, scope: Int, unit: Int) -> Float
): BoundsCriterion<T> {
    return object : BoundsCriterion<T>(direction) {

        override fun matchTarget(target: T, value: Distance): Boolean {
            return NumberRangeUtil.contains(value.rangeStart, value.rangeEnd) {
                bounds(target, value.scope, value.unit)
            }
        }
    }
}