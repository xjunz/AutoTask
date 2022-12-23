/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import java.lang.ref.WeakReference

private var previousToast: WeakReference<Toast>? = null

fun toast(any: Any?, length: Int = Toast.LENGTH_SHORT) {
    previousToast?.get()?.cancel()
    val toast = if (any is Int) {
        Toast.makeText(app, any, length)
    } else {
        Toast.makeText(app, any.toString(), length)
    }
    toast.show()
    previousToast = WeakReference(toast)
}

fun toastUnexpectedError(t: Throwable) {
    longToast(R.string.format_error_occurred.format("${t.javaClass.simpleName}: ${t.message}"))
}

suspend fun postToast(any: Any?, length: Int = Toast.LENGTH_SHORT) {
    withContext(Dispatchers.Main) {
        toast(any, length)
    }
}

fun longToast(any: Any?) = toast(any, Toast.LENGTH_LONG)