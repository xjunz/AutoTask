package top.xjunz.tasker.engine.criterion

import android.util.Range

/**
 * @author xjunz 2022/08/14
 */
class RangeCriterion<R : Any, T : Comparable<T>>(private val mapper: (R) -> T): Criterion<R, Range<T>>() {

    override fun matchTarget(target: R, value: Range<T>): Boolean {
        return value.contains(mapper(target))
    }

}