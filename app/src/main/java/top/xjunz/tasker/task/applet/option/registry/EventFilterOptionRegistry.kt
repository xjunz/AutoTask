package top.xjunz.tasker.task.applet.option.registry


import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.EventCriterion
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.task.applet.anno.AppletCategory

/**
 * @author xjunz 2022/08/12
 */
class EventFilterOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun eventFilterOption(@Event.EventType event: Int, label: Int) =
        appletOption(label) { EventCriterion(event) }.hasInnateValue()

    @AppletCategory(0)
    val pkgEntered =
        eventFilterOption(Event.EVENT_ON_PACKAGE_ENTERED, R.string.on_package_entered)

    @AppletCategory(1)
    val pkgExited =
        eventFilterOption(Event.EVENT_ON_PACKAGE_EXITED, R.string.on_package_left)

    @AppletCategory(2)
    val contentChanged =
        eventFilterOption(Event.EVENT_ON_CONTENT_CHANGED, R.string.on_content_changed)

    @AppletCategory(3)
    val notificationReceived =
        eventFilterOption(Event.EVENT_ON_NOTIFICATION_RECEIVED, R.string.on_notification_received)
            .withResult<String>(R.string.notification_content)

    override val categoryNames: IntArray? = null
}