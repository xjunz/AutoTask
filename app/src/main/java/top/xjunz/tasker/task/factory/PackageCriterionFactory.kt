package top.xjunz.tasker.task.factory

import top.xjunz.tasker.engine.criterion.*
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/08/09
 */
object PackageCriterionFactory : AppletFactory() {

    const val C_COLLECTION = "collection"

    const val C_STARTS_WITH = "startsWith"

    const val C_ENDS_WITH = "endsWith"

    const val C_PATTERN = "pattern"

    const val C_LENGTH_RANGE = "lengthRange"

    const val C_CONTAINS = "contains"

    override val name: String = "PackageCriterionFactory"

    override fun rawCreateApplet(name: String): Criterion<String, out Any> {

        return when (name) {

            C_COLLECTION -> CollectionCriterion()

            C_ENDS_WITH -> UnaryCriterion { target, value -> target.endsWith(value) }

            C_STARTS_WITH -> UnaryCriterion { target, value -> target.endsWith(value) }

            C_PATTERN -> BaseCriterion<String, Regex> { t, v -> t.matches(v) }

            C_LENGTH_RANGE -> RangeCriterion { it.length }

            C_CONTAINS -> UnaryCriterion { target, value -> target.contains(value) }

            else -> illegalArgument("name", name)

        }
    }

    override fun getDescriptionOf(applet: Applet): CharSequence? {
        TODO("Not yet implemented")
    }

    override fun getPromptOf(name: String): CharSequence {
        TODO("Not yet implemented")
    }

    override fun getLabelOf(name: String): CharSequence? {
        TODO("Not yet implemented")
    }

    override val supportedNames: Array<String> = arrayOf(
        C_COLLECTION, C_STARTS_WITH, C_ENDS_WITH,
        C_CONTAINS, C_PATTERN, C_LENGTH_RANGE
    )

}