package top.xjunz.tasker.engine.criterion

import kotlinx.serialization.Serializable

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

@Serializable
data class Distance(
    val scope: Int,
    val unit: Int,
    val portionScope: Int,
    val rangeStart: Float,
    val rangeEnd: Float
) {

    companion object {

        const val SCOPE_NONE = -1
        const val SCOPE_SCREEN = 0
        const val SCOPE_PARENT = 1

        const val UNIT_PX = 0
        const val UNIT_DP = 1
        const val UNIT_PORTION = 2

        const val NO_LIMIT = -1F

    }

}