/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.accessibilityservice.AccessibilityService
import android.os.Build
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.*
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.util.formatMinSecMills

/**
 * @author xjunz 2022/11/15
 */
class GlobalActionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun globalActionOption(title: Int, action: Int): AppletOption {
        return appletOption(title) {
            simpleAction {
                uiAutomation.performGlobalAction(action)
            }
        }
    }

    @AppletOrdinal(0x0000)
    val waitForIdle = appletOption(R.string.wait_for_idle) {
        singleValueAction<Int> {
            val xy = IntValueUtil.parseXY(it)
            currentService.a11yEventDispatcher.waitForIdle(xy.x.toLong(), xy.y.toLong())
        }
    }
        .withValueArgument<Int>(
            R.string.idle_threshold,
            variantValueType = VariantType.INT_INTERVAL
        )
        .withValueArgument<Int>(
            R.string.max_wait_duration,
            variantValueType = VariantType.INT_INTERVAL
        )
        .withHelperText(R.string.tip_wait_for_idle)
        .withValueDescriber<Int> {
            val xy = IntValueUtil.parseXY(it)
            R.string.format_wait_for_idle_desc.formatSpans(
                formatMinSecMills(xy.x).foreColored(),
                formatMinSecMills(xy.y).foreColored()
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
        R.string.open_notification_shade,
        AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
    )

    @AppletOrdinal(0x0005)
    val lockScreen = globalActionOption(
        R.string.lock_screen,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        else -1
    ).restrictApiLevel(Build.VERSION_CODES.P)

    @AppletOrdinal(0x0006)
    val takeScreenshot = globalActionOption(
        R.string.take_screenshot,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
        else -1
    ).restrictApiLevel(Build.VERSION_CODES.P)
}