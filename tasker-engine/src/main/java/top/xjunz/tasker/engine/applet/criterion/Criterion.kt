package top.xjunz.tasker.engine.applet.criterion

import kotlinx.serialization.Serializable
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The base criterion applet abstraction.
 *
 * @param T the target to be matched
 * @param V the value used to match a target
 *
 * @author xjunz 2022/08/06
 */
@Serializable
abstract class Criterion<T : Any, V : Any> : Applet() {

    /**
     * The default value when [value] is null.
     */
    open lateinit var defaultValue: V

    override suspend fun apply(runtime: TaskRuntime) {
        runtime.isSuccessful =
            isInverted != matchTarget(runtime.getTarget(), value?.casted() ?: defaultValue)
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}

/**
 * Create a criterion from a lambda.
 */
inline fun <T : Any, V : Any> newCriterion(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline matcher: ((T, V) -> Boolean)
): Criterion<T, V> {
    return object : Criterion<T, V>() {

        override var valueType: Int = valueType

        override fun matchTarget(target: T, value: V): Boolean {
            return matcher(target, value)
        }
    }
}