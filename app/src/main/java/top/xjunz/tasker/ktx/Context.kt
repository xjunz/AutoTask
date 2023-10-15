/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import java.io.File


/**
 * @author xjunz 2022/07/07
 */
fun Context.viewUrlSafely(url: String) {
    launchIntentSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/**
 * Returns the [Activity] given a [Context] or throw a exception if there is no [Activity],
 * taking into account the potential hierarchy of [ContextWrappers][ContextWrapper].
 */
fun Context.peekActivity(): AppCompatActivity? {
    var context = this
    if (this is AppCompatActivity) return this
    while (context is ContextWrapper) {
        if (context is AppCompatActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun File.makeContentUri(): Uri {
    return FileProvider.getUriForFile(app, "top.xjunz.tasker.provider.file", this)
}

fun Context.launchIntentSafely(intent: Intent): Boolean {
    return runCatching {
        if (peekActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }.onFailure {
        it.logcatStackTrace()
        toast(R.string.app_not_found)
    }.isSuccess
}