package top.xjunz.tasker.task.applet.option.registry


import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.EventCriterion
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption

/**
 * @author xjunz 2022/08/12
 */
class EventFilterOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun EventFilterOption(@Event.EventType event: Int, label: Int) =
        invertibleAppletOption(event, label, AppletOption.TITLE_NONE) {
            EventCriterion(event)
        }

    @AppletCategory(0)
    val pkgEntered =
        EventFilterOption(Event.EVENT_ON_PACKAGE_ENTERED, R.string.on_package_entered)

    @AppletCategory(1)
    val pkgExited =
        EventFilterOption(Event.EVENT_ON_PACKAGE_EXITED, R.string.on_package_left)

    @AppletCategory(2)
    val contentChanged =
        EventFilterOption(Event.EVENT_ON_CONTENT_CHANGED, R.string.on_content_changed)

    @AppletCategory(3)
    val notificationReceived =
        EventFilterOption(Event.EVENT_ON_NOTIFICATION_RECEIVED, R.string.on_notification_received)

    override val categoryNames: IntArray? = null
}