package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/01
 */
internal class DslFlow(private val initialTarget: Any? = null) : Flow() {

    override fun onPrepare(runtime: TaskRuntime) {
        if (initialTarget != null)
            runtime.setTarget(initialTarget)
    }
}