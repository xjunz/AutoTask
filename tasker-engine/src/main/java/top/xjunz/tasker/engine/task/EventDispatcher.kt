/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import androidx.collection.ArraySet
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.runtime.Event

/**
 * @author xjunz 2022/12/04
 */
abstract class EventDispatcher {

    private val callbacks = ArraySet<Callback>()

    abstract fun destroy()

    abstract fun onRegistered()

    open fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    open fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    fun dispatchEvents(vararg events: Event) {
        callbacks.forEach {
            it.onEvents(events.casted())
        }
    }

    fun interface Callback {

        fun onEvents(events: Array<Event>)

    }

}