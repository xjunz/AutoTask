package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeFlow : Flow() {

    override fun onPrepare(ctx: TaskContext, runtime: FlowRuntime) {
        super.onPrepare(ctx, runtime)
        runtime.setTarget(ctx.getOrPutArgument(id) {
            Calendar.getInstance().also {
                it.time = Date(System.currentTimeMillis())
            }
        })
    }
}