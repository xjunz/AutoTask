package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.criterion.EventCriterion
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
class When : ControlFlow() {

    override val requiredElementCount: Int = 1

    override val isRequired: Boolean = true

    companion object {

        fun ofEvent(@Event.EventType event: Int): When {
            return When().apply {
                if (isEmpty()) {
                    add(EventCriterion(event))
                } else {
                    this[0] = EventCriterion(event)
                }
            }
        }
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        if (!runtime.isSuccessful) {
            stopship(runtime)
        }
    }
}