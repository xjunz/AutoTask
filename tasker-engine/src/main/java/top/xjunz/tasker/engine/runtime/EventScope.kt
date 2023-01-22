/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2022/12/04
 */
class EventScope {

    val registry = mutableMapOf<Long, Any>()

    val failures = mutableMapOf<Applet, Pair<Any?, Any?>>()

    fun clear() {
        registry.clear()
        failures.clear()
    }

    fun registerFailure(applet: Applet, expected: Any?, actual: Any?) {
        failures[applet] = expected to actual
    }
}