/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

/**
 * @author xjunz 2022/12/04
 */
class Snapshot {

    val registry = mutableMapOf<Long, Any>()

    fun clear() {
        registry.clear()
    }
}