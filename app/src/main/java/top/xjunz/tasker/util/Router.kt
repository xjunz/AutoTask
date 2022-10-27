package top.xjunz.tasker.util

import android.content.Context
import top.xjunz.tasker.ktx.viewUrlSafely

/**
 * Helper class for routing app to a specific host.
 *
 * @author xjunz 2022/10/21
 */
object Router {

    const val SCHEME = "automator"
    const val HOST_ACCEPT_OPTIONS_FROM_INSPECTOR = "options-from-inspector"

    fun Context.routeTo(host: String) {
        viewUrlSafely("$SCHEME://$host")
    }
}