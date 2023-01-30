/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import androidx.core.util.Pools.SimplePool

/**
 * @author xjunz 2023/01/15
 */
class AppletResult private constructor(private var successful: Boolean) {

    val isSuccessful get() = successful

    var returns: Array<out Any?>? = null
        private set

    var actual: Any? = null
        private set

    var throwable: Throwable? = null
        private set

    private object Pool : SimplePool<AppletResult>(25)

    companion object {

        val SUCCESS = AppletResult(true)

        val FAILURE = AppletResult(false)

        private fun obtain(
            isSuccessful: Boolean,
            returns: Array<out Any?>? = null,
            actual: Any? = null,
            throwable: Throwable? = null
        ): AppletResult {
            return (Pool.acquire() ?: AppletResult(false)).also {
                it.successful = isSuccessful
                it.returns = returns
                it.actual = actual
                it.throwable = throwable
            }
        }

        fun succeeded(vararg returns: Any?): AppletResult {
            return if (returns.isNotEmpty()) obtain(true, returns) else SUCCESS
        }

        fun failed(actual: Any?): AppletResult {
            return obtain(false, actual = actual)
        }

        fun error(throwable: Throwable): AppletResult {
            return obtain(false, throwable = throwable)
        }
    }

    fun recycle() {
        returns = null
        actual = null
        throwable = null
        Pool.release(this)
    }
}