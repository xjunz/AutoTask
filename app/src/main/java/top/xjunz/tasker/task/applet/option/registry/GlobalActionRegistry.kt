/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.accessibilityservice.AccessibilityService
import android.os.Build
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.PowerManagerBridge
import top.xjunz.tasker.engine.applet.action.emptyArgAction
import top.xjunz.tasker.engine.applet.action.emptyArgOptimisticAction
import top.xjunz.tasker.engine.applet.action.singleNonNullArgAction
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.util.formatMinSecMills

/**
 * @author xjunz 2022/11/15
 */
class GlobalActionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun globalActionOption(title: Int, action: Int): AppletOption {
        return appletOption(title) {
            emptyArgAction {
                uiAutomation.performGlobalAction(action)
            }
        }
    }

    @AppletOrdinal(0x0000)
    val waitForIdle = appletOption(R.string.wait_for_idle) {
        singleNonNullArgAction<Int> {
            val xy = IntValueUtil.parseXY(it)
            uiAutomatorBridge.waitForIdle(xy.x.toLong(), xy.y.toLong())
        }
    }.withValueArgument<Int>(
        R.string.wait_for_idle, variantValueType = VariantArgType.INT_INTERVAL_XY
    ).withHelperText(R.string.tip_wait_for_idle).withSingleValueDescriber<Int> {
        val xy = IntValueUtil.parseXY(it)
        R.string.format_wait_for_idle_desc.formatSpans(
            formatMinSecMills(xy.x).foreColored(), formatMinSecMills(xy.y).foreColored()
        )
    }

    @AppletOrdinal(0x0001)
    val pressBack = globalActionOption(R.string.press_back, AccessibilityService.GLOBAL_ACTION_BACK)

    @AppletOrdinal(0x0002)
    val pressRecents = globalActionOption(
        R.string.press_recent, AccessibilityService.GLOBAL_ACTION_RECENTS
    )

    @AppletOrdinal(0x0003)
    val pressHome = globalActionOption(
        R.string.press_home, AccessibilityService.GLOBAL_ACTION_HOME
    )

    @AppletOrdinal(0x0004)
    val openNotificationShade = globalActionOption(
        R.string.open_notification_shade, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
    )

    @AppletOrdinal(0x0005)
    val lockScreen = globalActionOption(
        R.string.lock_screen,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        else -1
    ).restrictApiLevel(Build.VERSION_CODES.P)

    @AppletOrdinal(0x0006)
    val takeScreenshot = appletOption(R.string.take_screenshot) {
        emptyArgAction {
            uiAutomation.performGlobalAction(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                else -1
            )
        }
    }.restrictApiLevel(Build.VERSION_CODES.P)
        .premiumOnly()

    @AppletOrdinal(0x0007)
    val setRotation = appletOption(R.string.rotate_screen) {
        singleNonNullArgAction<Int> {
            uiAutomatorBridge.setRotation(it)
        }
    }.withValueArgument<Int>(R.string.rotation_direction, VariantArgType.INT_ROTATION).shizukuOnly()
        .withDescriber<Int> { applet, t ->
            R.string.format_desc_rotation_screen.formatSpans(
                R.array.rotations.array[t!!].clickToEdit(applet)
            )
        }.descAsTitle()

    @AppletOrdinal(0x0008)
    val wakeUpScreen = appletOption(R.string.wake_up_screen) {
        emptyArgOptimisticAction {
            PowerManagerBridge.wakeUpScreen()
        }
    }
}