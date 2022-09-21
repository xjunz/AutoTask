package top.xjunz.tasker.task.factory

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.criterion.EventFilter
import top.xjunz.tasker.engine.criterion.EventFilter.Companion.NAME_CONTENT_CHANGED
import top.xjunz.tasker.engine.criterion.EventFilter.Companion.NAME_PACKAGE_ENTERED
import top.xjunz.tasker.engine.criterion.EventFilter.Companion.NAME_PACKAGE_EXITED
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/08/12
 */
object EventFilterFactory : AppletFactory() {

    override val name: String = "EventCriterionFactory"

    override fun rawCreateApplet(name: String): Applet {
        return EventFilter(
            when (name) {
                NAME_CONTENT_CHANGED -> Event.EVENT_ON_CONTENT_CHANGED
                NAME_PACKAGE_ENTERED -> Event.EVENT_ON_PACKAGE_ENTERED
                NAME_PACKAGE_EXITED -> Event.EVENT_ON_PACKAGE_EXITED
                else -> illegalArgument("name", name)
            }
        )
    }

    override fun getDescriptionOf(applet: Applet): CharSequence? {
        return null
    }

    override fun getPromptOf(name: String): CharSequence {
        TODO("Not yet implemented")
    }

    override fun getLabelOf(name: String): CharSequence {
        return when (name) {
            NAME_CONTENT_CHANGED -> R.string.on_content_changed.text
            NAME_PACKAGE_ENTERED -> R.string.on_package_entered.text
            NAME_PACKAGE_EXITED -> R.string.on_package_exited.text
            else -> illegalArgument("name", name)
        }
    }

    override val supportedNames: Array<String> = arrayOf(
        NAME_PACKAGE_ENTERED, NAME_PACKAGE_EXITED, NAME_CONTENT_CHANGED
    )

}