package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/10/03
 */
@SerialName("RF")
class RelationalFlow : Flow() {

    companion object {
        const val RELATION_NONE = 0
        const val RELATION_AND = 1
        const val RELATION_OR = 2

        fun wrap(applet: Applet): RelationalFlow {
            return RelationalFlow().also {
                it.applets.add(applet)
            }
        }
    }

    override fun shouldDropFlow(ctx: AppletContext, runtime: FlowRuntime): Boolean {
        return when (relation) {
            RELATION_AND -> !runtime.isSuccessful
            RELATION_OR -> runtime.isSuccessful
            RELATION_NONE -> super.shouldDropFlow(ctx, runtime)
            else -> illegalArgument("relation", relation)
        }
    }

}