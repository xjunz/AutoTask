package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.newCriterion
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.NotificationFlow
import top.xjunz.tasker.task.applet.flow.PackageInfoContext
import top.xjunz.tasker.ui.task.selector.option.PackageInfoWrapper.Companion.wrapped
import top.xjunz.tasker.util.PackageInfoLoader

/**
 * @author xjunz 2022/11/16
 */
class NotificationOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    override val categoryNames: IntArray? = null

    @AppletCategory(0x00_00)
    val pkgCollection = invertibleAppletOption(0x00, R.string.in_notification_pkg_names) {
        collectionCriterion<NotificationFlow.NotificationInfo, String> {
            it.packageName
        }
    }.withDescriber<Collection<String>> {
        if (it.size == 1) {
            val first = it.first()
            PackageInfoLoader.loadPackageInfo(first)?.wrapped()?.label ?: first
        } else {
            R.string.format_pkg_collection_desc.format(
                it.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                    PackageInfoLoader.loadPackageInfo(name)?.wrapped()?.label ?: name
                }.joinToString("、"), it.size
            )
        }
    }

    @AppletCategory(0x00_01)
    val contentContains = invertibleAppletOption(0x02, R.string.notification_contains) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.panelTitle?.contains(v) == true
        }
    }

    @AppletCategory(0x00_03)
    val contentMatches = invertibleAppletOption(0x03, R.string.notification_matches) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.panelTitle?.matches(Regex(v)) == true
        }
    }

}