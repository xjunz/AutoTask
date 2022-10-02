package top.xjunz.tasker.engine.criterion

/**
 * @author xjunz 2022/09/22
 */
class PropertyCriterion<T : Any>(
    private inline val matcher: (target: T) -> Boolean
) : Criterion<T, Boolean>() {

    override fun matchTarget(target: T, value: Boolean): Boolean {
        return matcher(target) == value
    }
}