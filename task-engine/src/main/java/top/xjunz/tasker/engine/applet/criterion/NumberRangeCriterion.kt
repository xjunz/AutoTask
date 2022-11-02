package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.util.NumberRangeUtil

/**
 * @author xjunz 2022/08/14
 */
class NumberRangeCriterion<R : Any, T : Number>(
    rawType: Int = AppletValues.VAL_TYPE_INT,
    private inline val mapper: (R) -> T
) : Criterion<R, List<T>>() {

    override var valueType: Int = collectionTypeOf(rawType)

    override fun matchTarget(target: R, value: List<T>): Boolean {
        return NumberRangeUtil.contains(value[0], value[1]) {
            mapper(target)
        }
    }
}
