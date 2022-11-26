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
    const val HOST_ACTION = "action"

    fun Context.route(host: String) {
        viewUrlSafely("$SCHEME://$host")
    }

    fun Context.launchAction(actionName: String, value: Any) {
        query(HOST_ACTION, actionName to value)
    }

    fun Context.query(host: String, vararg queries: Pair<String, Any>) {
        check(queries.isNotEmpty()) {
            "No query provided!"
        }
        val query = StringBuilder()
        queries.forEach {
            query.append(it.first).append("=").append(it.second).append("&")
        }
        query.deleteCharAt(query.lastIndex)
        viewUrlSafely("$SCHEME://$host/?$query")
    }
}