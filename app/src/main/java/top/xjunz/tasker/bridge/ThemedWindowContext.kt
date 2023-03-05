/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */
package top.xjunz.tasker.bridge

import android.content.Context
import android.os.Binder
import android.view.WindowManagerImpl
import androidx.appcompat.view.ContextThemeWrapper
import top.xjunz.tasker.R
import top.xjunz.tasker.annotation.Privileged

@Privileged
class ThemedWindowContext(context: Context, private val binder: Binder) :
    ContextThemeWrapper(context, R.style.AppTheme) {

    override fun getSystemService(name: String): Any {
        if (Context.WINDOW_SERVICE != name) {
            return super.getSystemService(name)
        }
        val impl = super.getSystemService(name) as WindowManagerImpl
        impl.setDefaultToken(binder)
        return impl
    }
}