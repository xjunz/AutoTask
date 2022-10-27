package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext

/**
 * @author xjunz 2022/08/25
 */
class EventFilter(private val eventType: Int) : Applet() {

    override val valueType: Int = AppletValues.VAL_TYPE_INT

    override fun apply(context: TaskContext, runtime: FlowRuntime) {
        val hit = context.events.find { it.eventType == eventType }
        if (hit == null) {
            runtime.isSuccessful = false
        } else {
            runtime.isSuccessful = true
            runtime.setTarget(hit.targetPackage)
        }
    }

}