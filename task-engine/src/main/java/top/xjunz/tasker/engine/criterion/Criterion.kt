package top.xjunz.tasker.engine.criterion

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.util.illegalArgument

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

    private companion object {
        const val TYPE_STRING = 0
        const val TYPE_INT = 1
        const val TYPE_FLOAT = 2
        const val TYPE_BOOLEAN = 3
    }

    private var type = -1

    /**
     * The literal value for `serialization` and `deserialization`.
     */
    private lateinit var literal: String

    @Transient
    private lateinit var real: V

    fun requireValue(): Any {
        if (!::real.isInitialized && ::literal.isInitialized) {
            real = when (type) {
                TYPE_STRING -> literal
                TYPE_BOOLEAN -> literal.toBoolean()
                TYPE_FLOAT -> literal.toFloat()
                TYPE_INT -> literal.toInt()
                else -> illegalArgument("type", type)
            }.unsafeCast()
        }
        return real
    }

    fun setValue(value: Any) {
        real = value.unsafeCast()
        literal = value.toString()
        type = when (value) {
            is String -> TYPE_STRING
            is Int -> TYPE_INT
            is Boolean -> TYPE_BOOLEAN
            is Float -> TYPE_FLOAT
            else -> illegalArgument("type", value::class.java.name)
        }
    }

    override fun apply(context: AppletContext, runtime: FlowRuntime) {
        runtime.isSuccessful = isInverted != matchTarget(runtime.getTarget(), real)
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}