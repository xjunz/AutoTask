package top.xjunz.tasker.task.flow

import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.flow.Flow
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeFlow : Flow() {

    override fun onPrepare(ctx: AppletContext, runtime: FlowRuntime) {
        super.onPrepare(ctx, runtime)
        runtime.setTarget(ctx.getOrPutArgument(id) {
            Calendar.getInstance().also {
                it.time = Date(System.currentTimeMillis())
            }
        })
    }
}