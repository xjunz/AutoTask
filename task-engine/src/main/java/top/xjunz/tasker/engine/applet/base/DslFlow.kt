package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * @author xjunz 2022/11/01
 */
internal class DslFlow(private val initialTarget: Any? = null) : Flow() {

    override fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
        if (initialTarget != null)
            runtime.setTarget(initialTarget)
    }
}