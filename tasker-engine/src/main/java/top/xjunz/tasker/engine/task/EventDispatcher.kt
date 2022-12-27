/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import top.xjunz.tasker.engine.runtime.Event

/**
 * @author xjunz 2022/12/04
 */
abstract class EventDispatcher(private val callback: Callback) {

    fun dispatchEvents(vararg event: Event) {
        callback.onEvents(event)
    }

    fun interface Callback {
        fun onEvents(events: Array<out Event>)
    }

}