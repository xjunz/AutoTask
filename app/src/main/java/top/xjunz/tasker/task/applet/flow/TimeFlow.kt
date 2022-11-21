package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.FlowRuntime
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeFlow : Flow() {

    override fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
        super.onPrepare(task, runtime)
        runtime.setTarget(task.getOrPutCrossTaskVariable(id) {
            Calendar.getInstance().also {
                it.timeInMillis = System.currentTimeMillis()
            }
        })
    }
}