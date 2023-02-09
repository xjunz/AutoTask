/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.EMAIL_ADDRESS
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.util.formatCurrentTime
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
    return FileProvider.getUriForFile(app, "top.xjunz.automator.provider.file", this)
}

fun Context.shareFile(file: File) {
    val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, file.makeContentUri())
        .setType("*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    launchIntentSafely(intent)
}

fun Context.viewFile(file: File) {
    val intent = Intent(Intent.ACTION_VIEW, file.makeContentUri())
        .addCategory(Intent.CATEGORY_DEFAULT)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    launchIntentSafely(intent)
}

fun Context.launchIntentSafely(intent: Intent) {
    runCatching {
        if (peekActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }.onFailure {
        it.logcatStackTrace()
        toast(R.string.app_not_found)
    }
}

fun Context.pressHome() {
    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
}

fun Context.sendMailTo(log: Uri?) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(
        Intent.EXTRA_SUBJECT, R.string.mail_subject.format(formatCurrentTime())
    ).putExtra(Intent.EXTRA_TEXT, R.string.mail_body.format("TODO"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
    if (log != null) {
        intent.putExtra(Intent.EXTRA_STREAM, log)
        val resInfoList = packageManager.queryIntentActivities(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, log, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    launchIntentSafely(intent)
}