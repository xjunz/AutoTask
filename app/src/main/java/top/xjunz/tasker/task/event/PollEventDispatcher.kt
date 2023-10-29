/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.os.Looper
import androidx.core.os.HandlerCompat
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher

/**
 * @author xjunz 2023/09/11
 */
class PollEventDispatcher(looper: Looper) : EventDispatcher() {

    companion object {
        const val EXTRA_TICK_TIME_MILLS = 0
    }

    private var previousSec: Long = -1

    private val tickHandler by lazy {
        HandlerCompat.createAsync(looper)
    }

    override fun destroy() {
        previousSec = -1L
        tickHandler.removeCallbacksAndMessages(null)
    }

    private val tikTok: Runnable by lazy {
        Runnable {
            val event = Event.obtain(Event.EVENT_ON_TICK)
            val uptime = System.currentTimeMillis()
            val sec = uptime / 1000
            if (sec != previousSec) {
                previousSec = sec
                event.putExtra(EXTRA_TICK_TIME_MILLS, uptime)
                dispatchEvents(event)
            }
            tickHandler.postDelayed(tikTok, 1000L)
        }
    }

    override fun onRegistered() {
        tickHandler.post(tikTok)
    }

}