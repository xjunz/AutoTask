package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/25
 */
class EventCriterion(eventType: Int) : Applet() {

    init {
        value = eventType
    }

    override var valueType: Int = AppletValues.VAL_TYPE_INT

    override suspend fun apply(runtime: TaskRuntime) {
        val hit = runtime.events.find { it.type == value }
        if (hit == null) {
            runtime.isSuccessful = false
        } else {
            runtime.isSuccessful = true
            runtime.hitEvent = hit
        }
    }

}