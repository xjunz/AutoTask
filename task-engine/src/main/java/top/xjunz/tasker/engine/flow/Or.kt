package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult

/**
 * @author xjunz 2022/08/10
 */
@Serializable
@SerialName(Or.NAME)
class Or : Flow() {

    companion object {
        const val NAME = "Or"
    }

    @Transient
    override var name: String? = NAME

    override fun shouldDropFlow(ctx: AppletContext, result: AppletResult): Boolean {
        // When the previous result is true, drop this flow
        return result.isSuccessful
    }
}