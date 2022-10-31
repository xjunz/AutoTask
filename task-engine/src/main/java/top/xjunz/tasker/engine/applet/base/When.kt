package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.criterion.EventCriterion
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext

/**
 * @author xjunz 2022/08/11
 */
class When : Flow() {

    override val requiredElementCount: Int = 1

    override val isRequired: Boolean = true

    companion object {

        fun ofEvent(@Event.EventType event: Int): When {
            return When().apply {
                if (elements.isEmpty()) {
                    elements.add(EventCriterion(event))
                } else {
                    elements[0] = EventCriterion(event)
                }
            }
        }
    }

    override fun onPostApply(ctx: TaskContext, runtime: FlowRuntime) {
        super.onPostApply(ctx, runtime)
        if (!runtime.isSuccessful) {
            stopship(runtime)
        }
    }
}