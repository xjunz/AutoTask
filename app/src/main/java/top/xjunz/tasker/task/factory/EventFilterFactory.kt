package top.xjunz.tasker.task.factory


import androidx.annotation.StringRes
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.criterion.EventFilter
import top.xjunz.tasker.task.anno.AppletCategory

/**
 * @author xjunz 2022/08/12
 */
class EventFilterFactory : AppletFactory(AppletRegistry.ID_EVENT_FILTER_FACTORY) {

    private fun EventFilterOption(@Event.EventType event: Int, @StringRes label: Int) =
        AppletOption(event, label, LABEL_NONE) {
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

    override val label: Int = LABEL_NONE

    override val name: String = "EventCriterionFactory"

    override val categoryNames: IntArray = intArrayOf(LABEL_NONE)

}