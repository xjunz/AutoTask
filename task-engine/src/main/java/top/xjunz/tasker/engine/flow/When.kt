package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.criterion.EventFilter

/**
 * @author xjunz 2022/08/11
 */
@Serializable
@SerialName(When.NAME)
class When : Flow() {

    override val requiredElementCount: Int = 1

    override val isRequired: Boolean = true

    @Transient
    override var name: String? = NAME

    companion object {

        const val NAME = "When"

        fun ofEvent(@Event.EventType event: Int): When {
            return When().apply {
                if (applets.isEmpty()) {
                    applets.add(EventFilter(event))
                } else {
                    applets[0] = EventFilter(event)
                }
                applets[0].name = EventFilter.getNameOfEventType(event)
            }
        }
    }

    override fun onPostApply(ctx: AppletContext, result: AppletResult) {
        super.onPostApply(ctx, result)
        if (!result.isSuccessful) {
            stopship(ctx)
        }
    }
}