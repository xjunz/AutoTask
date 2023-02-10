/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.os.SystemClock
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewConfiguration

/**
 * @author xjunz 2022/11/29
 */
object ClickUtil {

    const val THRESHOLD_INTERVAL = 350

    fun View.setAntiMoneyClickListener(
        thresholdInterval: Int = THRESHOLD_INTERVAL,
        listener: (View) -> Unit
    ) {
        setOnClickListener(object : OnClickListener {

            var prevClickTimestamp = -1L

            override fun onClick(v: View) {
                val uptime = SystemClock.uptimeMillis()
                if (prevClickTimestamp == -1L || uptime - prevClickTimestamp >= thresholdInterval) {
                    listener(v)
                    prevClickTimestamp = uptime
                }
            }
        })
    }

    fun View.setOnDoubleClickListener(listener: (View) -> Unit) {
        setOnClickListener(object : OnClickListener {

            var prevClickTimestamp = -1L

            override fun onClick(v: View) {
                val uptime = SystemClock.uptimeMillis()
                if (prevClickTimestamp == -1L) {
                    prevClickTimestamp = uptime
                } else {
                    if (uptime - prevClickTimestamp <= ViewConfiguration.getDoubleTapTimeout()) {
                        listener(v)
                    } else {
                        prevClickTimestamp = uptime
                    }
                }
            }
        })
    }
}