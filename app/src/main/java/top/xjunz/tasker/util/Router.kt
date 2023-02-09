/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import top.xjunz.tasker.app
import top.xjunz.tasker.ui.MainActivity
import java.net.URLEncoder

/**
 * Helper class for routing app to a specific host.
 *
 * @author xjunz 2022/10/21
 */
object Router {

    private const val SCHEME = "xtsk"
    const val HOST_NONE = "no-op"
    const val HOST_ACTION = "action"

    fun Context.launchRoute(host: String) {
        routeTo("$SCHEME://$host")
    }

    fun Context.launchAction(actionName: String, value: Any) {
        launchQuery(actionName to value)
    }

    private fun Context.launchQuery(vararg queries: Pair<String, Any>) {
        check(queries.isNotEmpty()) {
            "No query provided!"
        }
        val query = StringBuilder()
        queries.forEach {
            query.append(it.first).append("=").append(
                URLEncoder.encode(it.second.toString(), "utf-8")
            ).append("&")
        }
        query.deleteCharAt(query.lastIndex)
        routeTo("$SCHEME://$HOST_ACTION/?$query")
    }

    private fun Context.routeTo(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW).setClass(app, MainActivity::class.java)
                .setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}