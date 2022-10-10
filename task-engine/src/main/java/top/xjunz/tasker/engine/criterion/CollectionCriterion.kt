package top.xjunz.tasker.engine.criterion

/**
 * @author xjunz 2022/08/14
 */
abstract class CollectionCriterion<T : Any, V : Any> : Criterion<T, Collection<V>>() {

    abstract fun T.getValue(): V

    override fun matchTarget(target: T, value: Collection<V>): Boolean {
        return value.contains(target.getValue())
    }
}

fun <T : Any, V : Any> CollectionCriterion(block: (T) -> V): CollectionCriterion<T, V> {
    return object : CollectionCriterion<T, V>() {
        override fun T.getValue(): V {
            return block(this)
        }
    }
}