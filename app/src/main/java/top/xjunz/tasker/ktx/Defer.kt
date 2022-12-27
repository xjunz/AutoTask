/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import kotlinx.coroutines.Deferred

/**
 * @author xjunz 2022/12/22
 */
inline fun Deferred<*>.invokeOnError(crossinline block: (Throwable) -> Unit) {
    invokeOnCompletion {
        if (it != null) block(it)
    }
}