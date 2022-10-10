package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.util.runtimeException

/**
 * @author xjunz 2022/08/11
 */
@SerialName("If")
open class If : Flow() {

    override fun checkElements() {
        super.checkElements()
        if (applets.size >= 1 && (applets[0].relation != RELATION_NONE)) {
            runtimeException("The first element or [If] must not be an [And] or an [Or].")
        }
    }

    override fun onPostApply(ctx: AppletContext, runtime: FlowRuntime) {
        if (isInverted == runtime.isSuccessful) {
            // The IF flow is failed.
            stopship(runtime)
        }
    }
}