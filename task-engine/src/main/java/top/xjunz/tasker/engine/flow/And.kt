package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime

/**
 * @author xjunz 2022/08/11
 */
@Serializable
@SerialName(And.NAME)
class And : Flow() {

    companion object {
        const val NAME = "And"
    }

    override fun shouldDropFlow(ctx: AppletContext, runtime: FlowRuntime): Boolean {
        // If the previous result is false, drop this flow
        return !runtime.isSuccessful
    }
}