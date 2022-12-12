package top.xjunz.tasker.task.applet.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/11/15
 */
class DelayAction : Action() {

    override val valueType: Int = AppletValues.VAL_TYPE_INT

    private var duration: Long = 0

    private var delayScope: WeakReference<CoroutineScope>? = null

    override suspend fun apply(runtime: TaskRuntime) {
        delayScope?.get()?.cancel()
        coroutineScope {
            delayScope = WeakReference(this)
            delay(duration)
        }
    }

}