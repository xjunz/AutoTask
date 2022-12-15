package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import androidx.core.content.pm.PackageInfoCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.NumberRangeCriterion
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.engine.applet.criterion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.newCriterion
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.ktx.isSystemApp
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.PackageInfoContext
import top.xjunz.tasker.ui.task.selector.option.PackageInfoWrapper.Companion.wrapped
import top.xjunz.tasker.util.PackageInfoLoader

/**
 * @author xjunz 2022/09/22
 */
class PackageOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletCategory(0x00_00)
    val pkgCollection = invertibleAppletOption(0x00, R.string.in_pkg_collection) {
        collectionCriterion<PackageInfoContext, String> {
            it.packageName
        }
    }.withValueDescriber<Collection<String>> { value ->
        if (value.size == 1) {
            val first = value.first()
            PackageInfoLoader.loadPackageInfo(first)?.wrapped()?.label ?: first
        } else {
            R.string.format_pkg_collection_desc.formatSpans(
                value.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                    (PackageInfoLoader.loadPackageInfo(name)?.wrapped()?.label ?: name)
                }.joinToString("、"), value.size.toString().foreColored()
            )
        }
    }

    @AppletCategory(0x00_01)
    val activityCollection = invertibleAppletOption(0x01, R.string.in_activity_collection) {
        collectionCriterion<PackageInfoContext, String> {
            it.activityName?.run {
                ComponentName.unflattenFromString(it.activityName)?.className
            }
        }
    }.withValueDescriber<Collection<String>> {
        if (it.size == 1) {
            it.first()
        } else {
            R.string.format_act_collection_desc.formatSpans(it.size.toString().foreColored())
        }
    }.withTitleModifier("Activity")

    @AppletCategory(0x00_02)
    val paneTitle = appletOption(0x02, R.string.with_pane_title) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.panelTitle == v
        }
    }

    @AppletCategory(0x01_00)
    private val isSystem = invertibleAppletOption(0x10, R.string.is_system) {
        PropertyCriterion<PackageInfoContext> {
            it.packageInfo.applicationInfo.isSystemApp
        }
    }

    @AppletCategory(0x01_01)
    private val isLauncher = invertibleAppletOption(0x11, R.string.is_launcher) {
        PropertyCriterion<PackageInfoContext> {
            it.packageName == uiAutomatorBridge.launcherPackageName
        }
    }

    @AppletCategory(0x01_02)
    private val versionRange = invertibleAppletOption(0x30, R.string.in_version_range) {
        NumberRangeCriterion<PackageInfoContext, Int> {
            PackageInfoCompat.getLongVersionCode(it.packageInfo).toInt()
        }
    }.withValueDescriber<Collection<Int>> {
        val first = it.firstOrNull()
        val last = it.lastOrNull()
        check(first != null || last != null)
        if (first == last) {
            first.toString()
        } else if (first == null && last != null) {
            R.string.format_less_than.format(last)
        } else if (first != null && last == null) {
            R.string.format_larger_than.format(first)
        } else {
            R.string.format_range.format(first, last)
        }
    }

    @AppletCategory(0x02_00)
    private val startsWith = invertibleAppletOption(0x40, R.string.pkg_name_starts_with) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.packageName.startsWith(v)
        }
    }

    @AppletCategory(0x02_01)
    private val endsWith = invertibleAppletOption(0x41, R.string.pkg_name_ends_with) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.packageName.endsWith(v)
        }
    }

    @AppletCategory(0x02_02)
    private val containsText = invertibleAppletOption(0x50, R.string.contains_text) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.packageName.contains(v)
        }
    }

    @AppletCategory(0x02_03)
    private val matchesPattern = invertibleAppletOption(0x60, R.string.pkg_name_matches_pattern) {
        newCriterion<PackageInfoContext, String>(AppletValues.VAL_TYPE_TEXT) { t, v ->
            t.packageName.matches(Regex(v))
        }
    }

    override val categoryNames: IntArray =
        intArrayOf(R.string.component_info, R.string.property, R.string.package_name)

}