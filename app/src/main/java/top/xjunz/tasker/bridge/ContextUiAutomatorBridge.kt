/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import android.view.ViewConfiguration
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.tasker.service.uiAutomation

/**
 * @author xjunz 2023/01/03
 */
abstract class ContextUiAutomatorBridge : UiAutomatorBridge(uiAutomation) {

    private val ctx: Context get() = ContextBridge.getContext()

    private val _scaledMinimumFlingVelocity: Int by lazy {
        ViewConfiguration.get(ctx).scaledMinimumFlingVelocity
    }

    private val powerManager by lazy {
        ctx.getSystemService(PowerManager::class.java)
    }

    final override fun getRotation(): Int {
        return defaultDisplay.rotation
    }

    final override fun isScreenOn(): Boolean {
        return powerManager.isInteractive
    }

    final override fun getDefaultDisplay(): Display {
        return DisplayManagerBridge.defaultDisplay
    }

    final override fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pm: PackageManager = ctx.packageManager
        val resolveInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        return resolveInfo?.activityInfo?.packageName
    }

    final override fun getScaledMinimumFlingVelocity(): Int {
        return _scaledMinimumFlingVelocity
    }

}