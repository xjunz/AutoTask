/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.os.Build
import android.os.PowerManager
import android.view.Display
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.InteractionController
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.tasker.service.a11yAutomatorService


/**
 * @author xjunz 2022/07/23
 */
class A11yUiAutomatorBridge(
    private val ctx: Context, uiAutomation: UiAutomation
) : UiAutomatorBridge(uiAutomation) {

    private val _scaledMinimumFlingVelocity: Int by lazy {
        ViewConfiguration.get(ctx).scaledMinimumFlingVelocity
    }

    private val powerManager by lazy {
        ctx.getSystemService(PowerManager::class.java)
    }

    private val windowManager by lazy {
        ctx.getSystemService(WindowManager::class.java)
    }

    override fun getInteractionController(): InteractionController {
        return A11yInteractionController(a11yAutomatorService, this)
    }

    override fun getRotation(): Int {
        return defaultDisplay.rotation
    }

    override fun isScreenOn(): Boolean {
        return powerManager.isInteractive
    }

    override fun getDefaultDisplay(): Display {
        return DisplayManagerBridge.defaultDisplay
    }

    override fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pm: PackageManager = ctx.packageManager
        val resolveInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(
                    intent, ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        return resolveInfo?.activityInfo?.packageName
    }

    override fun getScaledMinimumFlingVelocity(): Int {
        return _scaledMinimumFlingVelocity
    }

    override fun getGestureController(device: UiDevice): GestureController {
        return A11yGestureController(a11yAutomatorService, device)
    }
}