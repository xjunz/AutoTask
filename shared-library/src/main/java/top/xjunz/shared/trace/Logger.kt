/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.shared.trace

import android.util.Log

/**
 * @author xjunz 2022/07/14
 */

private const val DEF_LOGCAT_TAG = "AutomatorTrace"

fun logcat(any: Any?, priority: Int = Log.INFO, tag: String = DEF_LOGCAT_TAG) {
    Log.println(priority, tag, any.toString())
}

fun Throwable.logcatStackTrace() {
    Log.e(DEF_LOGCAT_TAG, Log.getStackTraceString(this))
}

fun <V> V.logit(priority: Int = Log.INFO, tag: String = DEF_LOGCAT_TAG): V {
    logcat(this, priority, tag)
    return this
}