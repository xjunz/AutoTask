package top.xjunz.tasker.task.applet.flow

import kotlinx.coroutines.delay
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/15
 */
class DelayAction : Action<Int>(AppletValues.VAL_TYPE_INT) {

    override suspend fun doAction(value: Int?, runtime: TaskRuntime): Boolean {
        delay(value!!.toLong())
        return true
    }

}