/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.xjunz.tasker.engine.runtime.Event
import java.util.*

/**
 * @author xjunz 2022/12/04
 */
abstract class EventDispatcher : CoroutineScope {

    private val callbacks = LinkedList<Callback>()

    abstract fun destroy()

    fun addCallback(callback: Callback) {
        callbacks.offer(callback)
    }

    fun dispatchEvents(vararg events: Event) {
        launch {
            callbacks.forEach {
                it.onEvents(events)
            }
        }
    }

    fun interface Callback {
        suspend fun onEvents(events: Array<out Event>)
    }

}