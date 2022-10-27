package top.xjunz.tasker.task.applet.option.registry


import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.EventFilter
import top.xjunz.tasker.engine.value.Event
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption

/**
 * @author xjunz 2022/08/12
 */
class EventFilterOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun EventFilterOption(@Event.EventType event: Int, label: Int) =
        AppletOption(event, label, AppletOption.TITLE_NONE) {
            EventFilter(event)
        }

    @AppletCategory(0)
    private val pkgEntered =
        EventFilterOption(Event.EVENT_ON_PACKAGE_ENTERED, R.string.on_package_entered)

    @AppletCategory(1)
    private val pkgExited =
        EventFilterOption(Event.EVENT_ON_PACKAGE_EXITED, R.string.on_package_exited)

    @AppletCategory(2)
    private val contentChanged =
        EventFilterOption(Event.EVENT_ON_CONTENT_CHANGED, R.string.on_content_changed)

    override val title: Int = AppletOption.TITLE_NONE

    override val categoryNames: IntArray = intArrayOf(AppletOption.TITLE_NONE)
}