package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.dto.AppletValues

/**
 * @author xjunz 2022/08/14
 */
abstract class CollectionCriterion<T : Any, V : Any>(rawType: Int) :
    Criterion<T, Collection<V>>() {

    override val valueType: Int = collectionTypeOf(rawType)

    abstract fun T.getValue(): V?

    final override fun matchTarget(target: T, value: Collection<V>): Boolean {
        return value.contains(target.getValue())
    }
}

inline fun <T : Any, V : Any> collectionCriterion(
    type: Int = AppletValues.VAL_TYPE_TEXT,
    crossinline block: (T) -> V?
): CollectionCriterion<T, V> {
    return object : CollectionCriterion<T, V>(type) {
        override fun T.getValue(): V? {
            return block(this)
        }
    }
}