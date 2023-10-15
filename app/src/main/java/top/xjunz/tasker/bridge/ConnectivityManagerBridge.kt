/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * @author xjunz 2023/10/13
 */
object ConnectivityManagerBridge {

    private val cm by lazy {
        ContextBridge.getContext().getSystemService(ConnectivityManager::class.java)
    }

    val isNetworkAvailable: Boolean get() = cm.activeNetwork != null

    private fun getActiveNetworkCapabilities(): NetworkCapabilities? {
        val network = cm.activeNetwork ?: return null
        return cm.getNetworkCapabilities(network)
    }

    val isWifi: Boolean
        get() {
            return getActiveNetworkCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                ?: false
        }

    val isCellular: Boolean
        get() {
            return getActiveNetworkCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                ?: false
        }

}