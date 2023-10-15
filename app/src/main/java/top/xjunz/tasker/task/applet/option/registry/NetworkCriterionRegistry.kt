/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ConnectivityManagerBridge
import top.xjunz.tasker.bridge.WifiManagerBridge
import top.xjunz.tasker.engine.applet.criterion.booleanCriterion
import top.xjunz.tasker.engine.applet.criterion.unaryEqualCriterion
import top.xjunz.tasker.task.applet.anno.AppletOrdinal

/**
 * @author xjunz 2023/10/13
 */
class NetworkCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0001)
    val isNetworkActive = invertibleAppletOption(R.string.is_network_available) {
        booleanCriterion {
            ConnectivityManagerBridge.isNetworkAvailable
        }
    }

    @AppletOrdinal(0x0002)
    val isWifiNetwork = invertibleAppletOption(R.string.is_wifi_network) {
        booleanCriterion {
            ConnectivityManagerBridge.isWifi
        }
    }

    @AppletOrdinal(0x0003)
    val isCellularNetwork = invertibleAppletOption(R.string.is_cellular_network) {
        booleanCriterion {
            ConnectivityManagerBridge.isCellular
        }
    }

    @AppletOrdinal(0x0004)
    val currentWifiSsidIs = invertibleAppletOption(R.string.current_wifi_ssid_is) {
        unaryEqualCriterion {
            WifiManagerBridge.getCurrentConnectedWifiSSID()
        }
    }.shizukuOnly()
}