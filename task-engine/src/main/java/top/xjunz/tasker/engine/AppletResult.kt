package top.xjunz.tasker.engine

import android.util.ArrayMap
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.engine.flow.Flow


/**
 * @author xjunz 2022/08/09
 */
class AppletResult(events: Array<Event>) {

    /**
     * All results yield by the middle-flows
     */
    private val flowResults = ArrayMap<Flow, Any>()

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    var depth = 0

    /**
     * The return value of the current applet.
     */
    private var value: Any = events

    fun setValue(any: Any) {
        value = any
    }

    fun getRawValue(): Any {
        return value
    }

    fun <T> getValue(): T {
        return value.unsafeCast()
    }

    fun <T : Any> checkValueType(): Boolean {
        return runCatching { value.unsafeCast<T>() }.isSuccess
    }

    fun getResultBy(name: String) {

    }
}