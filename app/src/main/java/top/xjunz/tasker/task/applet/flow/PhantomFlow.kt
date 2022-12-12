package top.xjunz.tasker.task.applet.flow

import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * A flow which is intended to be merged into its parent flow. This flow only exists in edition time.
 *
 * @author xjunz 2022/11/10
 */
class PhantomFlow : Flow() {

    override fun onPrepare(runtime: TaskRuntime) {
        unsupportedOperation("PhantomFlow is not expected to be present in runtime!")
    }

    override fun staticCheckMySelf() {
        unsupportedOperation("PhantomFlow should be merged!")
    }

}