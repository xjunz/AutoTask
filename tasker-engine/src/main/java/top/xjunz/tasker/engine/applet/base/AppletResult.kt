/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import androidx.core.util.Pools.SynchronizedPool

/**
 * @author xjunz 2023/01/15
 */
class AppletResult private constructor(private var successful: Boolean) {

    val isSuccessful get() = successful

    var returned: Any? = null

    var actual: Any? = null

    var throwable: Throwable? = null

    private object Pool : SynchronizedPool<AppletResult>(16)

    companion object {

        val EMPTY_SUCCESS = AppletResult(true)

        val EMPTY_FAILURE = AppletResult(false)

        fun obtain(
            isSuccessful: Boolean,
            returned: Any? = null,
            actual: Any? = null,
            throwable: Throwable? = null
        ): AppletResult {
            return (Pool.acquire() ?: AppletResult(false)).also {
                it.successful = isSuccessful
                it.returned = returned
                // Boolean value is unnecessary
                it.actual = if (actual is Boolean) null else actual
                it.throwable = throwable
            }
        }

        fun succeeded(returned: Any?): AppletResult {
            return if (returned == null) EMPTY_SUCCESS else obtain(true, returned)
        }

        fun failed(actual: Any?): AppletResult {
            return if (actual == null) EMPTY_FAILURE else obtain(false, actual = actual)
        }

        fun error(throwable: Throwable): AppletResult {
            return obtain(false, throwable = throwable)
        }

        fun emptyResult(success: Boolean): AppletResult {
            return if (success) EMPTY_SUCCESS else EMPTY_FAILURE
        }

        /**
         * Make a result as per a criterion.
         */
        inline fun <T> resultOf(actualValue: T, isSuccessful: (T) -> Boolean): AppletResult {
            return if (isSuccessful(actualValue)) {
                // Also store actual value for successful result, because the result
                // may be inverted to failure, where we need the actual value.
                obtain(true, null, actualValue, null)
            } else {
                failed(actualValue)
            }
        }

        fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
        }
    }

    fun recycle() {
        if (this === EMPTY_SUCCESS || this === EMPTY_FAILURE) return
        returned = null
        actual = null
        throwable = null
        Pool.release(this)
    }
}