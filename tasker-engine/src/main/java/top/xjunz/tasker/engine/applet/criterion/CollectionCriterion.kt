package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.dto.AppletValues

/**
 * @author xjunz 2022/08/14
 */
class CollectionCriterion<T : Any, V : Any>(
    rawType: Int,
    private inline val getValue: T.() -> V?
) : Criterion<T, Collection<V>>() {

    override val valueType: Int = collectionTypeOf(rawType)

    override fun matchTarget(target: T, value: Collection<V>): Boolean {
        return value.contains(target.getValue())
    }
}

inline fun <T : Any, reified V : Any> collectionCriterion(noinline block: (T) -> V?)
        : CollectionCriterion<T, V> {
    return CollectionCriterion(AppletValues.judgeValueType<V>(), block)
}