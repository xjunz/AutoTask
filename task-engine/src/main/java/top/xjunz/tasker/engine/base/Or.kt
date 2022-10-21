package top.xjunz.tasker.engine.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime

/**
 * @author xjunz 2022/08/10
 */
@Serializable
@SerialName("Or")
class Or : Flow() {

    override fun shouldDropFlow(ctx: AppletContext, runtime: FlowRuntime): Boolean {
        // When the previous result is true, drop this flow
        return runtime.isSuccessful
    }
}