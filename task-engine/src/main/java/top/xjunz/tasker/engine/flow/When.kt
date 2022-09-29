package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.criterion.EventFilter

/**
 * @author xjunz 2022/08/11
 */
@Serializable
@SerialName("When")
class When : Flow() {

    override val requiredElementCount: Int = 1

    override val isRequired: Boolean = true

    companion object {

        fun ofEvent(@Event.EventType event: Int): When {
            return When().apply {
                if (applets.isEmpty()) {
                    applets.add(EventFilter(event))
                } else {
                    applets[0] = EventFilter(event)
                }
            }
        }
    }

    override fun onPostApply(ctx: AppletContext, runtime: FlowRuntime) {
        super.onPostApply(ctx, runtime)
        if (!runtime.isSuccessful) {
            stopship(runtime)
        }
    }
}