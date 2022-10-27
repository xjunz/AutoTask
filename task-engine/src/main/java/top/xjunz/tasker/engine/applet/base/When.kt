package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.criterion.EventFilter
import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext
import top.xjunz.tasker.engine.value.Event

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
                    elements.add(EventFilter(event))
                } else {
                    elements[0] = EventFilter(event)
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