package top.xjunz.tasker.engine.base

import kotlinx.serialization.SerialName
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime

/**
 * @author xjunz 2022/08/11
 */
@SerialName("If")
open class If : Flow() {

    override fun onPostApply(ctx: AppletContext, runtime: FlowRuntime) {
        if (isInverted == runtime.isSuccessful) {
            // The IF flow is failed.
            stopship(runtime)
        }
    }
}