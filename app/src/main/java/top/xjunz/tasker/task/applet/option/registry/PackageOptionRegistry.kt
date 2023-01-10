/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import androidx.core.content.pm.PackageInfoCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.criterion.*
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.ktx.isSystemApp
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.criterion.numberRangeCriterion
import top.xjunz.tasker.task.applet.flow.ComponentInfoContext
import top.xjunz.tasker.ui.model.PackageInfoWrapper.Companion.wrapped

/**
 * @author xjunz 2022/09/22
 */
class PackageOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x00_00)
    val pkgCollection = invertibleAppletOption(R.string.in_pkg_collection) {
        collectionCriterion<ComponentInfoContext, String> {
            it.packageName
        }
    }.withValueDescriber<Collection<String>> { value ->
        if (value.size == 1) {
            val first = value.first()
            PackageManagerBridge.loadPackageInfo(first)?.wrapped()?.label ?: first
        } else {
            R.string.format_pkg_collection_desc.formatSpans(
                value.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                    (PackageManagerBridge.loadPackageInfo(name)?.wrapped()?.label ?: name)
                }.joinToString("、"), value.size.toString().foreColored()
            )
        }
    }

    @AppletOrdinal(0x00_01)
    val activityCollection = invertibleAppletOption(R.string.in_activity_collection) {
        collectionCriterion<ComponentInfoContext, String> {
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

    @AppletOrdinal(0x00_02)
    val paneTitle = appletOption(R.string.with_pane_title) {
        newCriterion<ComponentInfoContext, String> { t, v ->
            t.panelTitle == v
        }
    }

    @AppletOrdinal(0x01_00)
    private val isSystem = invertibleAppletOption(R.string.is_system) {
        PropertyCriterion<ComponentInfoContext> {
            it.packageInfo.applicationInfo.isSystemApp
        }
    }

    @AppletOrdinal(0x01_01)
    private val isLauncher = invertibleAppletOption(R.string.is_launcher) {
        PropertyCriterion<ComponentInfoContext> {
            it.packageName == uiAutomatorBridge.launcherPackageName
        }
    }

    @AppletOrdinal(0x01_02)
    private val versionRange = invertibleAppletOption(R.string.in_version_range) {
        numberRangeCriterion<ComponentInfoContext, Int> {
            PackageInfoCompat.getLongVersionCode(it.packageInfo).toInt()
        }
    }.withDefaultRangeDescriber()

    @AppletOrdinal(0x02_00)
    private val startsWith = invertibleAppletOption(R.string.pkg_name_starts_with) {
        newCriterion<ComponentInfoContext, String> { t, v ->
            t.packageName.startsWith(v)
        }
    }

    @AppletOrdinal(0x02_01)
    private val endsWith = invertibleAppletOption(R.string.pkg_name_ends_with) {
        newCriterion<ComponentInfoContext, String> { t, v ->
            t.packageName.endsWith(v)
        }
    }

    @AppletOrdinal(0x02_02)
    private val containsText = invertibleAppletOption(R.string.pkg_name_contains_text) {
        newCriterion<ComponentInfoContext, String> { t, v ->
            t.packageName.contains(v)
        }
    }

    @AppletOrdinal(0x02_03)
    private val matchesPattern = invertibleAppletOption(R.string.pkg_name_matches_pattern) {
        newCriterion<ComponentInfoContext, String> { t, v ->
            t.packageName.matches(Regex(v))
        }
    }

    override val categoryNames: IntArray =
        intArrayOf(R.string.component_info, R.string.property, R.string.package_name)

}