/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.content.Context
import android.content.ContextHidden
import android.os.UserHandleHidden
import android.util.Log
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.utils.OsUtil
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.annotation.Anywhere
import top.xjunz.tasker.app
import top.xjunz.tasker.isAppProcess
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 * @author xjunz 2023/01/03
 */
@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
object ContextBridge {

    private const val SHELL_APPLICATION_ID = "com.android.shell"

    @Anywhere
    fun getContext(): Context {
        return if (isAppProcess) app else createPrivilegedContext()
    }

    @Anywhere
    fun getAppContext(): Context {
        return if (isAppProcess) app else createAppContext()
    }

    private fun createAppContext(): Context {
        val activityThread: ActivityThread = ActivityThread.currentActivityThread()
        val sysCtx: Context = activityThread.systemContext
        val opPkgName = if (OsUtil.isInShellProcess) SHELL_APPLICATION_ID else "android"
        return try {
            val ctx: Context = sysCtx.casted<ContextHidden>()
                .createPackageContextAsUser(BuildConfig.APPLICATION_ID, 0, UserHandleHidden.of(0))
            val cls = Class.forName("android.app.ContextImpl")
            val method: Method = cls.getDeclaredMethod(
                "createAppContext",
                ActivityThread::class.java, Class.forName("android.app.LoadedApk"),
                String::class.java
            )
            method.isAccessible = true
            val field: Field = cls.getDeclaredField("mPackageInfo")
            field.isAccessible = true
            method.invoke(null, activityThread, field.get(ctx), opPkgName) as Context
        } catch (th: Throwable) {
            Log.e("Context", Log.getStackTraceString(th))
            sysCtx
        }
    }

    private fun createPrivilegedContext(): Context {
        val activityThread: ActivityThread = ActivityThread.currentActivityThread()
        val sysCtx: Context = activityThread.systemContext
        return if (OsUtil.isInRootProcess) {
            sysCtx
        } else try {
            val ctx: Context =
                sysCtx.casted<ContextHidden>().createPackageContextAsUser(
                    SHELL_APPLICATION_ID, 0, UserHandleHidden.of(0)
                )
            val cls = Class.forName("android.app.ContextImpl")
            val method: Method = cls.getDeclaredMethod(
                "createAppContext",
                ActivityThread::class.java, Class.forName("android.app.LoadedApk"),
                String::class.java
            )
            method.isAccessible = true
            val field = cls.getDeclaredField("mPackageInfo")
            field.isAccessible = true
            method.invoke(null, activityThread, field.get(ctx), SHELL_APPLICATION_ID) as Context
        } catch (th: Throwable) {
            Log.e("Context", Log.getStackTraceString(th))
            sysCtx
        }
    }
}