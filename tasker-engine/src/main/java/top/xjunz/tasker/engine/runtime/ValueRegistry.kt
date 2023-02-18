/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import android.util.ArrayMap
import top.xjunz.shared.ktx.casted
import java.util.*

/**
 * @author xjunz 2023/02/16
 */
open class ValueRegistry {

    class WeakKey

    private var weakRegistry: WeakHashMap<WeakKey, Any>? = null

    private var registry: MutableMap<Any, Any>? = null

    @Synchronized
    private fun initializeOrRequireRegistry(): MutableMap<Any, Any> {
        if (registry == null) {
            registry = ArrayMap()
        }
        return registry!!
    }

    @Synchronized
    private fun initializeOrRequireWeakRegistry(): MutableMap<WeakKey, Any> {
        if (weakRegistry == null) {
            weakRegistry = WeakHashMap()
        }
        return weakRegistry!!
    }

    @Synchronized
    fun <V : Any> getWeakValue(key: WeakKey, initializer: () -> V): V {
        return initializeOrRequireWeakRegistry().getOrPut(key, initializer).casted()
    }

    @Synchronized
    fun <V : Any> getValue(key: Any, initializer: () -> V): V {
        return initializeOrRequireRegistry().getOrPut(key, initializer).casted()
    }

    fun clear() {
        registry = null
        weakRegistry = null
    }
}