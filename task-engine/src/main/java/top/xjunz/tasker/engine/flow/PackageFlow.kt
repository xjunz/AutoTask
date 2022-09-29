package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime

/**
 * @author xjunz 2022/09/03
 */
@Serializable
@SerialName(PackageFlow.NAME)
class PackageFlow : If() {

    companion object {
        const val NAME = "PackageFlow"
    }

    override fun onPreApply(ctx: AppletContext, runtime: FlowRuntime) {
        super.onPreApply(ctx, runtime)
    }

}