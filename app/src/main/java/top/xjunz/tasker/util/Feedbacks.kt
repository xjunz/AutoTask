/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import rikka.shizuku.Shizuku
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.launchIntentSafely

/**
 * @author xjunz 2023/03/06
 */
object Feedbacks {

    private fun dumpEnvInfo() = buildString {
        appendLine("Basic:")
        appendLine("--version code = ${BuildConfig.VERSION_CODE}")
        appendLine("--version name = ${BuildConfig.VERSION_NAME}")
        appendLine("--android version = ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})")
        appendLine("--brand & model = ${Build.BRAND} ${Build.MODEL}")
        appendLine("--supported abi = ${Build.SUPPORTED_ABIS.joinToString()}")

        appendLine("Shizuku:")
        val shizukuBinderReceived = Shizuku.pingBinder()
        appendLine("--ping binder = $shizukuBinderReceived")
        if (shizukuBinderReceived) {
            appendLine("--shizuku version = ${Shizuku.getVersion()}")
            appendLine("--uid = ${Shizuku.getUid()}")
        }
    }

    fun feedbackByEmail(file: Uri?) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(
            Intent.EXTRA_SUBJECT, R.string.mail_subject.format(formatCurrentTime())
        ).putExtra(Intent.EXTRA_TEXT, R.string.mail_body.format(dumpEnvInfo()))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf("webackup.feedback@gmail.com"))
        if (file != null) {
            intent.putExtra(Intent.EXTRA_STREAM, file)
            @Suppress("DEPRECATION")
            val resInfoList = app.packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                app.grantUriPermission(packageName, file, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        app.launchIntentSafely(intent)
    }
}