/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.net.wifi.WifiManager


/**
 * @author xjunz 2023/10/12
 */
object WifiManagerBridge {

    private val wm: WifiManager by lazy {
        ContextBridge.getContext().getSystemService(WifiManager::class.java)
    }

    @Suppress("DEPRECATION")
    fun getCurrentConnectedWifiSSID(): String? {
        return wm.connectionInfo?.ssid
    }

}