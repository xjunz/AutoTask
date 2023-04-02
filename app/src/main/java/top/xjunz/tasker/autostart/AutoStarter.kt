/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import rikka.shizuku.Shizuku
import top.xjunz.tasker.service.controller.ShizukuAutomatorServiceController
import top.xjunz.tasker.util.ShizukuUtil

/**
 * @author xjunz 2021/8/16
 */
class AutoStarter : BroadcastReceiver() {

    private val oneShotBinderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            Shizuku.removeBinderReceivedListener(this)
            if (ShizukuUtil.isShizukuAvailable) {
                // TODO: Show countdown as a notification
                ShizukuAutomatorServiceController.bindServiceOnBoot()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED
            || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            Shizuku.addBinderReceivedListenerSticky(oneShotBinderReceivedListener)
        }
    }
}