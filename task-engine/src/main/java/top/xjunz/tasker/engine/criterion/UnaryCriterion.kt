package top.xjunz.tasker.engine.criterion

/**
 * @author xjunz 2022/08/14
 */
class UnaryCriterion<T : Any>(
    private inline val matcher: (target: T, value: T) -> Boolean
) : Criterion<T, T>() {

    override fun matchTarget(target: T, value: T): Boolean {
        return matcher.invoke(target, value)
    }
}