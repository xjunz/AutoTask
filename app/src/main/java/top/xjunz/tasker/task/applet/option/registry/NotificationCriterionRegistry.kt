/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.criterion.CollectionCriterion.Companion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.LambdaCriterion.Companion.newCriterion
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.flow.NotificationFlow
import top.xjunz.tasker.task.applet.value.VariantType

/**
 * @author xjunz 2022/11/16
 */
class NotificationCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x00_00)
    val appCollection = invertibleAppletOption(R.string.in_notification_pkg_names) {
        collectionCriterion<NotificationFlow.NotificationTarget, String> {
            it.packageName
        }
    }.withValueArgument<String>(R.string.app_collection, VariantType.TEXT_APP_LIST)
        .withValueDescriber<Collection<String>> {
            if (it.size == 1) {
                val first = it.first()
                PackageManagerBridge.loadLabelOfPackage(first)
            } else {
                R.string.format_pkg_collection_desc.format(
                    it.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                        PackageManagerBridge.loadLabelOfPackage(name)
                    }.joinToString("、"), it.size
                )
            }
        }

    @AppletOrdinal(0x00_01)
    val contentContains = invertibleAppletOption(R.string.notification_contains) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.paneTitle?.contains(v) == true
        }
    }

    @AppletOrdinal(0x00_02)
    val contentMatches = invertibleAppletOption(R.string.notification_matches) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.paneTitle?.matches(Regex(v)) == true
        }
    }

}