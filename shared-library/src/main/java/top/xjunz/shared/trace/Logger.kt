package top.xjunz.tasker.trace

import android.util.Log

/**
 * @author xjunz 2022/07/14
 */

private const val DEF_LOGCAT_TAG = "AutomatorTrace"

fun logcat(any: Any?, priority: Int = Log.INFO, tag: String = DEF_LOGCAT_TAG) {
    Log.println(priority, tag, any.toString())
}

fun Any?.logit(priority: Int = Log.INFO, tag: String = DEF_LOGCAT_TAG) {
    logcat(this, priority, tag)
}