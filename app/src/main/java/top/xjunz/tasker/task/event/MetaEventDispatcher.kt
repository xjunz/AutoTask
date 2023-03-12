/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.util.ArrayMap
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.task.EventDispatcher

/**
 * @author xjunz 2023/03/12
 */
class MetaEventDispatcher : EventDispatcher() {

    private val allDispatchers = ArrayMap<Class<*>, EventDispatcher>()

    fun registerEventDispatcher(eventDispatcher: EventDispatcher) {
        allDispatchers[eventDispatcher::class.java] = eventDispatcher
    }

    fun <T> getEventDispatcher(clz: Class<T>): T {
        return allDispatchers.getValue(clz).casted()
    }

    override fun addCallback(callback: Callback) {
        allDispatchers.values.forEach {
            it.addCallback(callback)
        }
    }

    override fun removeCallback(callback: Callback) {
        allDispatchers.values.forEach {
            it.removeCallback(callback)
        }
    }

    override fun destroy() {
        allDispatchers.values.forEach {
            it.destroy()
        }
    }
}