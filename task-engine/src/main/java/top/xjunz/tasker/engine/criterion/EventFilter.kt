package top.xjunz.tasker.engine.criterion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.flow.Applet

/**
 * @author xjunz 2022/08/25
 */
@Serializable
@SerialName("Event")
class EventFilter(private val eventType: Int) : Applet() {

    override fun apply(context: AppletContext, runtime: FlowRuntime) {
        val hit = context.events.find { it.eventType == eventType }
        if (hit == null) {
            runtime.isSuccessful = false
        } else {
            runtime.isSuccessful = true
            runtime.setTarget(hit.targetPackage)
        }
    }

}