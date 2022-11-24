package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeFlow : Flow() {

    override fun onPrepare(runtime: TaskRuntime) {
        super.onPrepare(runtime)
        runtime.setTarget(runtime.getOrPutCrossTaskVariable(id) {
            Calendar.getInstance().also {
                it.timeInMillis = System.currentTimeMillis()
            }
        })
    }
}