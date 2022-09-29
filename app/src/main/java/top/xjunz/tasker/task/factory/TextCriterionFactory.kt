package top.xjunz.tasker.task.factory

import top.xjunz.tasker.engine.criterion.BaseCriterion
import top.xjunz.tasker.engine.criterion.CollectionCriterion
import top.xjunz.tasker.engine.criterion.RangeCriterion
import top.xjunz.tasker.engine.criterion.UnaryCriterion
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/08/09
 */
abstract class TextCriterionFactory(id: Int) : AppletFactory(id) {

    companion object {

        const val C_COLLECTION = "collection"

        const val C_STARTS_WITH = "startsWith"

        const val C_ENDS_WITH = "endsWith"

        const val C_PATTERN = "pattern"

        const val C_LENGTH_RANGE = "lengthRange"

        const val C_CONTAINS = "contains"
    }

    override val name: String = "TextCriterionFactory"

    fun rawCreateApplet(appletName: String): Applet {

        return when (appletName) {

            C_COLLECTION -> CollectionCriterion<String>()

            C_ENDS_WITH -> UnaryCriterion<String> { target, value -> target.endsWith(value) }

            C_STARTS_WITH -> UnaryCriterion<String> { target, value -> target.endsWith(value) }

            C_PATTERN -> BaseCriterion<String, Regex> { t, v -> t.matches(v) }

            C_LENGTH_RANGE -> RangeCriterion<String, Int> { it.length }

            C_CONTAINS -> UnaryCriterion<String> { target, value -> target.contains(value) }

            else -> illegalArgument("name", appletName)

        }
    }
}