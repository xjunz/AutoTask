package top.xjunz.tasker.engine.criterion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.util.illegalArgument

/**
 * The base criterion abstraction.
 *
 * @param T the target to be matched
 * @param V the value used to match a target
 *
 * @author xjunz 2022/08/06
 */
@Serializable
abstract class Criterion<T : Any, V : Any> : Applet() {

    @Serializable
    @SerialName("Val")
    class Val<V : Any>() {

        companion object {
            const val TYPE_STRING = 0
            const val TYPE_INT = 1
            const val TYPE_FLOAT = 2
            const val TYPE_BOOLEAN = 3
        }

        constructor(value: V, isInverted: Boolean = false) : this() {
            this.what = value
            this.isInverted = isInverted
        }

        /**
         * Whether to invert the match result.
         */
        var isInverted: Boolean = false

        private var type = -1

        private lateinit var literal: String

        @Transient
        private var _what: V? = null

        var what: V
            get() {
                if (_what == null && ::literal.isInitialized) {
                    _what = when (type) {
                        TYPE_STRING -> literal
                        TYPE_BOOLEAN -> literal.toBoolean()
                        TYPE_FLOAT -> literal.toFloat()
                        TYPE_INT -> literal.toInt()
                        else -> illegalArgument("type", type)
                    }.unsafeCast()
                }
                return requireNotNull(_what)
            }
            set(value) {
                _what = value
                literal = value.toString()
                type = when (value) {
                    is String -> TYPE_STRING
                    is Int -> TYPE_INT
                    is Boolean -> TYPE_BOOLEAN
                    is Float -> TYPE_FLOAT
                    else -> illegalArgument("type", value::class.java.simpleName)
                }
            }

    }

    lateinit var value: Val<V>

    override fun apply(context: AppletContext, sharedResult: AppletResult) {
        // When inverted, matchTarget() is expected to return false and vice versa
        sharedResult.isSuccessful =
            value.isInverted != matchTarget(sharedResult.getValue(), value.what)
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}