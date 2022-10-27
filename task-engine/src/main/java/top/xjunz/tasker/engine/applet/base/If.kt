package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext

/**
 * @author xjunz 2022/08/11
 */
open class If : Flow() {

    override fun onPostApply(ctx: TaskContext, runtime: FlowRuntime) {
        if (isInverted == runtime.isSuccessful) {
            // The IF flow is failed.
            stopship(runtime)
        }
    }
}