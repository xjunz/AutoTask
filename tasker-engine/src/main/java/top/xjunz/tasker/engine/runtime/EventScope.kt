/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import android.util.ArrayMap

/**
 * @author xjunz 2022/12/04
 */
class EventScope {

    val registry = ArrayMap<Long, Any>()

    fun clear() {
        registry.clear()
    }
}