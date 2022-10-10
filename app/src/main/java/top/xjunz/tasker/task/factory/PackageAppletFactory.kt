package top.xjunz.tasker.task.factory

import androidx.core.content.pm.PackageInfoCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.criterion.BaseCriterion
import top.xjunz.tasker.engine.criterion.CollectionCriterion
import top.xjunz.tasker.engine.criterion.PropertyCriterion
import top.xjunz.tasker.engine.criterion.RangeCriterion
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.isSystemApp
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.task.anno.AppletCategory
import top.xjunz.tasker.task.flow.PackageInfoContext
import top.xjunz.tasker.ui.task.editor.selector.PackageInfoWrapper.Companion.wrapped
import top.xjunz.tasker.util.PackageInfoLoader

/**
 * @author xjunz 2022/09/22
 */
class PackageAppletFactory(id: Int) : AppletFactory(id) {

    companion object {
        const val APPLET_PKG_COLLECTION = 0x00
        const val APPLET_ACT_COLLECTION = 0x01
    }

    @AppletCategory(0x00_00)
    private val pkgCollection = AppletOption(APPLET_PKG_COLLECTION, R.string.in_pkg_collection) {
        CollectionCriterion<PackageInfoContext, String> {
            it.packageName
        }
    }.withDescriber<Collection<String>> {
        R.string.format_pkg_collection_desc.format(
            it.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                PackageInfoLoader.loadPackageInfo(name)?.wrapped()?.label ?: name
            }.joinToString("、"), it.size
        )
    }

    @AppletCategory(0x00_01)
    private val activityCollection =
        AppletOption(APPLET_ACT_COLLECTION, R.string.in_activity_collection) {
            CollectionCriterion<PackageInfoContext, String> {
                it.activityName
            }
        }.withDescriber<Collection<String>> {
            R.string.format_act_collection_desc.format(it.size)
        }


    @AppletCategory(0x01_00)
    private val isSystem = AppletOption(0x10, R.string.is_system) {
        PropertyCriterion<PackageInfoContext> {
            it.packageInfo.applicationInfo.isSystemApp
        }
    }

    @AppletCategory(0x01_01)
    private val isLauncher = AppletOption(0x11, R.string.is_launcher) {
        PropertyCriterion<PackageInfoContext> {
            it.packageName == currentService.uiAutomatorBridge.launcherPackageName
        }
    }

    @AppletCategory(0x01_02)
    private val versionRange = AppletOption(0x30, R.string.in_version_range) {
        RangeCriterion<PackageInfoContext, Long> {
            PackageInfoCompat.getLongVersionCode(it.packageInfo)
        }
    }

    @AppletCategory(0x02_00)
    private val startsWith = AppletOption(0x40, R.string.starts_with) {
        BaseCriterion<PackageInfoContext, String> { t, v -> t.packageName.startsWith(v) }
    }

    @AppletCategory(0x02_01)
    private val endsWith = AppletOption(0x41, R.string.ends_with) {
        BaseCriterion<PackageInfoContext, String> { t, v -> t.packageName.endsWith(v) }
    }

    @AppletCategory(0x02_02)
    private val containsText = AppletOption(0x50, R.string.contains_text) {
        BaseCriterion<PackageInfoContext, String> { t, v -> t.packageName.contains(v) }
    }

    @AppletCategory(0x02_03)
    private val matchesPattern = AppletOption(0x60, R.string.matches_pattern) {
        BaseCriterion<PackageInfoContext, String> { t, v -> t.packageName.matches(Regex(v)) }
    }

    override val title: Int = R.string.current_package_matches

    override val categoryNames: IntArray =
        intArrayOf(R.string.component_name, R.string.property, R.string.match_package_name)

}