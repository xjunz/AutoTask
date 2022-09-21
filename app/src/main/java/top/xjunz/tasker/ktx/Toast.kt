/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.widget.Toast
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

fun longToast(any: Any?) = toast(any, Toast.LENGTH_LONG)