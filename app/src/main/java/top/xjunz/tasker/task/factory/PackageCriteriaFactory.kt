package top.xjunz.tasker.task.factory

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.criterion.CheckCriteria
import top.xjunz.tasker.engine.criterion.CollectionCriterion
import top.xjunz.tasker.engine.criterion.RangeCriterion
import top.xjunz.tasker.engine.criterion.UnaryCriterion
import top.xjunz.tasker.ktx.isSystemApp
import top.xjunz.tasker.service.controller.currentService
import top.xjunz.tasker.task.anno.AppletCategory

/**
 * @author xjunz 2022/09/22
 */
class PackageCriteriaFactory : AppletFactory(AppletRegistry.ID_PKG_APPLET_FACTORY) {

    @AppletCategory(0x00_00)
    private val isSystem = AppletOption(0, R.string.is_system) {
        CheckCriteria<PackageInfo> {
            it.applicationInfo.isSystemApp
        }
    }

    @AppletCategory(0x00_01)
    private val isLauncher = AppletOption(0x10, R.string.is_launcher) {
        CheckCriteria<PackageInfo> {
            it.packageName == currentService.context.launcherPackageName
        }
    }

    @AppletCategory(0x00_02)
    private val versionRange = AppletOption(0x20, R.string.in_version_range) {
        RangeCriterion<PackageInfo, Long> {
            PackageInfoCompat.getLongVersionCode(it)
        }
    }

    @AppletCategory(0x01_03)
    private val inCollection = AppletOption(0x30, R.string.in_pkg_collection) {
        CollectionCriterion<String>()
    }

    @AppletCategory(0x02_00)
    private val startsWith = AppletOption(0x40, R.string.in_pkg_collection) {
        UnaryCriterion<String> { t, v -> t.startsWith(v) }
    }

    @AppletCategory(0x02_01)
    private val endsWith = AppletOption(0x50, R.string.in_pkg_collection) {
        UnaryCriterion<String> { t, v -> t.endsWith(v) }
    }

    @AppletCategory(0x02_02)
    private val containsText = AppletOption(0x60, R.string.contains_text) {
        UnaryCriterion<String> { t, v -> t.contains(v) }
    }

    @AppletCategory(0x02_03)
    private val matchesPattern = AppletOption(0x70, R.string.matches_pattern) {
        UnaryCriterion<String> { t, v -> t.matches(Regex(v)) }
    }

    override val label: Int = R.string.current_package_name

    override val name: String = "PackageCriteriaFactory"

    override val categoryNames: IntArray =
        intArrayOf(R.string.basics, R.string.accurate_match, R.string.fuzzy_match)

}