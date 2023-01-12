/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import android.content.Intent
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ContextBridge
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.action.unaryArgAction
import top.xjunz.tasker.privileged.ActivityManagerUtil
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.ui.model.PackageInfoWrapper.Companion.wrapped

/**
 * @author xjunz 2023/01/06
 */
class ApplicationActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0001)
    val forceStopApp = appletOption(R.string.format_force_stop) {
        unaryArgAction<String> {
            ActivityManagerUtil.forceStopPackage(it)
            true
        }
    }.withArgument<String>(R.string.specified_app)
        .hasCompositeTitle()
        .withValueDescriber<String> {
            PackageManagerBridge.loadPackageInfo(it)?.wrapped()?.label ?: it
        }.shizukuOnly()

    @AppletOrdinal(0x0002)
    val launchApp = appletOption(R.string.format_launch) {
        unaryArgAction<String> {
            ContextBridge.getContext().startActivity(
                requireNotNull(PackageManagerBridge.getLaunchIntentFor(it)) {
                    "Launch intent for [$it] not found!"
                }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }
    }.hasCompositeTitle()
        .withArgument<String>(R.string.specified_app)
        .withValueDescriber<String> {
            PackageManagerBridge.loadPackageInfo(it)?.wrapped()?.label ?: it
        }

    @AppletOrdinal(0x0003)
    val launchActivity = appletOption(R.string.format_launch) {
        unaryArgAction<String> {
            ContextBridge.getContext().startActivity(
                Intent().setComponent(ComponentName.unflattenFromString(it))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }
    }.withArgument<String>(R.string.specified_activity)
        .hasCompositeTitle()

}