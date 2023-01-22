/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.os.SystemClock
import android.view.View
import android.view.View.OnClickListener

/**
 * @author xjunz 2022/11/29
 */
object AntiMonkeyUtil {

    inline fun View.setAntiMoneyClickListener(
        thresholdInterval: Int = 350,
        crossinline listener: (View) -> Unit
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
}