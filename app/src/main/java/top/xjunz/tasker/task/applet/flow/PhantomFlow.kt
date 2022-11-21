package top.xjunz.tasker.task.applet.flow

import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * A flow which is intended to be merged into its parent flow. This flow only exists in edition time.
 *
 * @author xjunz 2022/11/10
 */
class PhantomFlow : ControlFlow() {

    override fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
        unsupportedOperation("PhantomFlow is not expected to be present in runtime!")
    }

    override fun staticCheckMySelf() {
        unsupportedOperation("PhantomFlow should be merged!")
    }

}