package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult

/**
 * @author xjunz 2022/08/11
 */
@Serializable
@SerialName(And.NAME)
class And : Flow() {

    companion object {
        const val NAME = "And"
    }

    @Transient
    override var name: String? = NAME

    override fun shouldDropFlow(ctx: AppletContext, result: AppletResult): Boolean {
        // If the previous result is false, drop this flow
        return !result.isSuccessful
    }
}