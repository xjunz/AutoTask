/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.autostart

import android.content.ComponentName
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.system.Os
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.service.premiumContext
import top.xjunz.tasker.util.ShizukuUtil

/**
 * @author xjunz 2021/8/16
 */

object AutoStartUtil {

    private val packageManager get() = app.packageManager

    private val shizukuAutoStartComponentName by lazy {
        ComponentName(MANAGER_APPLICATION_ID, "moe.shizuku.manager.starter.BootCompleteReceiver")
    }

    private val myAutoStartComponentName by lazy {
        ComponentName(
            BuildConfig.APPLICATION_ID + premiumContext.empty,
            AutoStarter::class.java.name + premiumContext.empty
        )
    }

    private fun enableShizukuAutoStart() = runCatching {
        val ipm = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        ipm.setComponentEnabledSetting(
            shizukuAutoStartComponentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP,
            Os.getuid() / 100_000
        )
    }.isSuccess

    val isAutoStartEnabled
        get() = isPremium && isComponentEnabled(
            myAutoStartComponentName, false
        ) && isShizukuAutoStartEnabled

    fun toggleAutoStart(enabled: Boolean) {
        if (isAutoStartEnabled == enabled) return
        if (enabled && !isShizukuAutoStartEnabled && (!ShizukuUtil.isShizukuAvailable || !enableShizukuAutoStart())) {
            toast(R.string.tip_enable_shizuku_auto_start)
            ShizukuUtil.launchShizukuManager()
            return
        }
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(
            myAutoStartComponentName, newState, PackageManager.DONT_KILL_APP
        )
    }

    private inline val isShizukuAutoStartEnabled
        get() = isComponentEnabled(shizukuAutoStartComponentName, true)

    private fun isComponentEnabled(componentName: ComponentName, def: Boolean): Boolean {
        return try {
            when (packageManager.getComponentEnabledSetting(componentName)) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> def
                else -> false
            }
        } catch (t: Throwable) {
            false
        }
    }
}