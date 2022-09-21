package top.xjunz.tasker.engine.criterion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/08/25
 */
@Serializable
@SerialName("Event")
class EventFilter(private val eventType: Int) : Applet() {

    companion object {

        const val NAME_PACKAGE_EXITED = "pkgExited"

        const val NAME_PACKAGE_ENTERED = "pkgEntered"

        const val NAME_CONTENT_CHANGED = "contentChanged"

        fun getNameOfEventType(@Event.EventType eventType: Int): String {
            return when (eventType) {
                Event.EVENT_ON_PACKAGE_ENTERED -> NAME_PACKAGE_ENTERED
                Event.EVENT_ON_CONTENT_CHANGED -> NAME_CONTENT_CHANGED
                Event.EVENT_ON_PACKAGE_EXITED -> NAME_PACKAGE_EXITED
                else -> illegalArgument("eventType", eventType)
            }
        }
    }


    override fun apply(context: AppletContext, sharedResult: AppletResult) {
        val hit = context.events.find { it.eventType == eventType }
        if (hit == null) {
            sharedResult.isSuccessful = false
        } else {
            sharedResult.isSuccessful = true
            sharedResult.setValue(hit.targetPackage)
        }
    }

}