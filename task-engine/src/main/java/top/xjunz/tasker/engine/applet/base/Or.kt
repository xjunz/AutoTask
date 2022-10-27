package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext

/**
 * @author xjunz 2022/08/10
 */
class Or : Flow() {

    override fun shouldDropFlow(ctx: TaskContext, runtime: FlowRuntime): Boolean {
        // When the previous result is true, drop this flow
        return runtime.isSuccessful
    }
}