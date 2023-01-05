/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.accessibilityservice.AccessibilityService
import android.os.Build
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.*
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption

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

    @AppletCategory(0x0000)
    val pressBack = globalActionOption(R.string.press_back, AccessibilityService.GLOBAL_ACTION_BACK)

    @AppletCategory(0x0001)
    val pressRecents = globalActionOption(
        R.string.press_recent, AccessibilityService.GLOBAL_ACTION_RECENTS
    )

    @AppletCategory(0x0002)
    val pressHome = globalActionOption(
        R.string.press_home, AccessibilityService.GLOBAL_ACTION_HOME
    )

    @AppletCategory(0x0003)
    val openNotificationShade = globalActionOption(
        R.string.open_notification_shade,
        AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
    )

    @AppletCategory(0x0004)
    val lockScreen = globalActionOption(
        R.string.lock_screen,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        else -1
    ).restrictApiLevel(Build.VERSION_CODES.P)

    @AppletCategory(0x0005)
    val takeScreenshot = globalActionOption(
        R.string.take_screenshot,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
        else -1
    ).restrictApiLevel(Build.VERSION_CODES.P)
}