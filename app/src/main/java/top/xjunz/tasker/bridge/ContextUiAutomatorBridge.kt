/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.app.UiAutomation
import android.content.Context
import android.os.PowerManager
import android.view.Display
import android.view.ViewConfiguration
import top.xjunz.tasker.uiautomator.CoroutineUiAutomatorBridge

/**
 * @author xjunz 2023/01/03
 */
abstract class ContextUiAutomatorBridge(uiAutomation: UiAutomation) :
    CoroutineUiAutomatorBridge(uiAutomation) {

    private val ctx: Context get() = ContextBridge.getContext()

    private val powerManager by lazy {
        ctx.getSystemService(PowerManager::class.java)
    }

    override val rotation: Int get() = defaultDisplay.rotation

    override val isScreenOn: Boolean get() = powerManager.isInteractive

    override val defaultDisplay: Display get() = DisplayManagerBridge.defaultDisplay

    override val launcherPackageName: String? get() = PackageManagerBridge.getLauncherPackageName()

    override val scaledMinimumFlingVelocity: Int by lazy {
        ViewConfiguration.get(ctx).scaledMinimumFlingVelocity
    }

}