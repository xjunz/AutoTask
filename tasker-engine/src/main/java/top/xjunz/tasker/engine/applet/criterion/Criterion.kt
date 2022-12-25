package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The base criterion applet abstraction.
 *
 * @param T the target to be matched
 * @param V the value used to match a target
 *
 * @author xjunz 2022/08/06
 */
abstract class Criterion<T : Any, V : Any> : Applet() {

    /**
     * The default value when [value] is null.
     */
    open lateinit var defaultValue: V

    final override suspend fun apply(runtime: TaskRuntime) {
        runtime.isSuccessful =
            isInverted != matchTarget(runtime.getTarget(), value?.casted() ?: defaultValue)
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}

class LambdaCriterion<T : Any, V : Any>(
    override val valueType: Int,
    private inline val matcher: ((T, V) -> Boolean)
) : Criterion<T, V>() {

    override fun matchTarget(target: T, value: V): Boolean {
        return matcher(target, value)
    }
}

inline fun <T : Any, reified V : Any> newCriterion(noinline matcher: ((T, V) -> Boolean))
        : Criterion<T, V> {
    return LambdaCriterion(AppletValues.judgeValueType<V>(), matcher)
}