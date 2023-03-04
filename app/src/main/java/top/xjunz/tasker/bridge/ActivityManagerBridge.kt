/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.app
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.ktx.execShellCmd
import top.xjunz.tasker.premium.PremiumMixin

/**
 * @author xjunz 2022/11/15
 */
object ActivityManagerBridge {

    fun forceStopPackage(pkgName: String) {
        SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
            .execShellCmd("force-stop", pkgName)
    }

    fun startComponent(componentName: ComponentName) {
        if (isAppProcess) {
            app.startActivity(
                Intent().setComponent(componentName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
                .execShellCmd("start", "-n", componentName.flattenToString())
        }
    }

    fun startComponent(componentName: String) {
        PremiumMixin.ensurePremium()
        if (isAppProcess) {
            app.startActivity(
                Intent().setComponent(ComponentName.unflattenFromString(componentName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
                .execShellCmd("start", "-n", componentName)
        }
    }
}