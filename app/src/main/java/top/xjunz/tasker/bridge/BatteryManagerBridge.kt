/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.os.BatteryManager

/**
 * @author xjunz 2022/11/10
 */
object BatteryManagerBridge {

    private val batteryManager: BatteryManager by lazy {
        ContextBridge.getContext().getSystemService(BatteryManager::class.java)
    }

    val isCharging: Boolean get() = batteryManager.isCharging

    val capacity: Int
        get() {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
}