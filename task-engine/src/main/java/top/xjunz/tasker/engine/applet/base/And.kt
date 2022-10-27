package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext

/**
 * @author xjunz 2022/08/11
 */
class And : Flow() {

    override fun shouldDropFlow(ctx: TaskContext, runtime: FlowRuntime): Boolean {
        // If the previous result is false, drop this flow
        return !runtime.isSuccessful
    }
}