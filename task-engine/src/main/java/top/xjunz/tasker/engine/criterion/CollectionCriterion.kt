package top.xjunz.tasker.engine.criterion

/**
 * @author xjunz 2022/08/14
 */
class CollectionCriterion<T : Any> : Criterion<T, Collection<T>>() {

    override fun matchTarget(target: T, value: Collection<T>): Boolean {
        return value.contains(target)
    }
}