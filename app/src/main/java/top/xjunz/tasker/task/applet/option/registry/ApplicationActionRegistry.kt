/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ActivityManagerBridge
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.action.*
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.value.VariantType

/**
 * @author xjunz 2023/01/06
 */
class ApplicationActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0000)
    val forceStopApp = appletOption(R.string.format_force_stop) {
        singleArgAction<ComponentInfoWrapper> {
            checkNotNull(it)
            ActivityManagerBridge.forceStopPackage(it.packageName)
            true
        }
    }.withRefArgument<ComponentInfoWrapper>(R.string.specified_app)
        .hasCompositeTitle().shizukuOnly()

    @AppletOrdinal(0x0001)
    val launchApp = appletOption(R.string.format_launch) {
        binaryArgValueAction<String, ComponentInfoWrapper>({
            packageName
        }) {
            ActivityManagerBridge.startComponent(
                requireNotNull(PackageManagerBridge.getLaunchIntentFor(it)?.component) {
                    "Launch intent for $it not found!"
                }
            )
            true
        }
    }.hasCompositeTitle()
        .withBinaryArgument<String, ComponentInfoWrapper>(
            R.string.specified_app,
            variantValueType = VariantType.TEXT_PACKAGE_NAME
        )
        .withValueDescriber<String> {
            PackageManagerBridge.loadLabelOfPackage(it)
        }

    @AppletOrdinal(0x0002)
    val launchActivity = appletOption(R.string.launch_activity) {
        valueAction<String> {
            ActivityManagerBridge.startComponent(it)
            true
        }
    }.withValueArgument<String>(R.string.activity, VariantType.TEXT_ACTIVITY)
        .withValueDescriber<String> {
            val comp = ComponentName.unflattenFromString(it)!!
            PackageManagerBridge.loadLabelOfPackage(comp.packageName)
                .toString() + "/" + it.substringAfterLast('/')
        }

}