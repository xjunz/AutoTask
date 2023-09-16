/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.annotation.SuppressLint
import android.os.PowerManager
import android.os.PowerManager.WakeLock

/**
 * @author xjunz 2023/09/11
 */
object PowerManagerBridge {

    private val powerManager by lazy {
        ContextBridge.getContext().getSystemService(PowerManager::class.java)
    }

    @SuppressLint("WakelockTimeout")
    @Suppress("DEPRECATION")
    fun wakeUpScreen() {
        val wakeLock = powerManager.newWakeLock(
            PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK,
            "xjunz:AutoTask:WakeUp"
        )
        wakeLock.acquire()
        wakeLock.release()
    }

    fun obtainWakeLock(): WakeLock {
        return powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "xjunz:AutoTask:Service"
        )
    }
}