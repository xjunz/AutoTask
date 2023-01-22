/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import androidx.core.util.Pools.SimplePool

/**
 * @author xjunz 2023/01/15
 */
class AppletResult private constructor(
    private var _isSuccessful: Boolean
) {

    val isSuccessful get() = _isSuccessful

    var values: Array<out Any?>? = null
        private set

    var expected: Any? = null
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
            values: Array<out Any?>? = null,
            expected: Any? = null,
            actual: Any? = null,
            throwable: Throwable? = null
        ): AppletResult {
            return (Pool.acquire() ?: AppletResult(false)).also {
                it._isSuccessful = isSuccessful
                it.values = values
                it.actual = actual
                it.expected = expected
                it.throwable = throwable
            }
        }

        fun successWithReturn(vararg results: Any?): AppletResult {
            return if (results.isNotEmpty()) obtain(true, results) else SUCCESS
        }

        fun failure(expected: Any?, actual: Any?): AppletResult {
            return obtain(false, expected = expected, actual = actual)
        }

        fun error(throwable: Throwable): AppletResult {
            return obtain(false, throwable = throwable)
        }
    }

    fun recycle() {
        values = null
        expected = null
        actual = null
        throwable = null
        Pool.release(this)
    }
}