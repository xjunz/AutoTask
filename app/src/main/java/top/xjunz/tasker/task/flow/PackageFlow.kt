package top.xjunz.tasker.task.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.base.If

/**
 * @author xjunz 2022/09/03
 */
@Serializable
@SerialName("PackageFlow")
class PackageFlow : If() {

    override fun onPrepare(ctx: AppletContext, runtime: FlowRuntime) {
        super.onPreApply(ctx, runtime)
        val info = ctx.getOrPutArgument(id) {
            PackageInfoContext(ctx.currentPackageName, ctx.currentActivityName)
        }
        runtime.setTarget(info)
    }

}