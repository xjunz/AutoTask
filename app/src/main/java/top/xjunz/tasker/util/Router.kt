package top.xjunz.tasker.util

import android.content.Context
import top.xjunz.tasker.ktx.viewUrlSafely

/**
 * Helper class for routing app to a specific host.
 *
 * @author xjunz 2022/10/21
 */
object Router {

    private const val SCHEME = "automator"
    const val HOST_ACCEPT_OPTIONS_FROM_INSPECTOR = "options-from-inspector"
    const val HOST_NONE = "no-op"

    fun Context.routeTo(host: String) {
        viewUrlSafely("$SCHEME://$host")
    }
}