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
    const val HOST_ACCEPT_NODE_INFO_FROM_INSPECTOR = "node-info"

    fun Context.routeTo(host: String) {
        viewUrlSafely("$SCHEME://$host")
    }
}