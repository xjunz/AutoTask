/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.criterion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.newCriterion
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.NotificationFlow
import top.xjunz.tasker.task.applet.flow.PackageInfoContext
import top.xjunz.tasker.ui.task.selector.option.PackageInfoWrapper.Companion.wrapped

/**
 * @author xjunz 2022/11/16
 */
class NotificationOptionRegistry(id: Int) : AppletOptionRegistry(id) {



    @AppletCategory(0x00_00)
    val pkgCollection = invertibleAppletOption(R.string.in_notification_pkg_names) {
        collectionCriterion<NotificationFlow.NotificationInfo, String> {
            it.packageName
        }
    }.withValueDescriber<Collection<String>> {
        if (it.size == 1) {
            val first = it.first()
            PackageManagerBridge.loadPackageInfo(first)?.wrapped()?.label ?: first
        } else {
            R.string.format_pkg_collection_desc.format(
                it.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                    PackageManagerBridge.loadPackageInfo(name)?.wrapped()?.label ?: name
                }.joinToString("、"), it.size
            )
        }
    }

    @AppletCategory(0x00_01)
    val contentContains = invertibleAppletOption(R.string.notification_contains) {
        newCriterion<PackageInfoContext, String> { t, v ->
            t.panelTitle?.contains(v) == true
        }
    }

    @AppletCategory(0x00_03)
    val contentMatches = invertibleAppletOption(R.string.notification_matches) {
        newCriterion<PackageInfoContext, String> { t, v ->
            t.panelTitle?.matches(Regex(v)) == true
        }
    }

}