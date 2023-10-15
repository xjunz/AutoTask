/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import top.xjunz.tasker.engine.applet.base.AppletResult

/**
 * @author xjunz 2023/02/12
 */
interface Referent {

    fun getReferredValue(which: Int, runtime: TaskRuntime): Any? {
        throw NullPointerException("Field $which is not found!")
    }

    fun asResult(): AppletResult {
        return AppletResult.succeeded(this)
    }
}