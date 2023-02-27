/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.content.Context
import android.content.ContextHidden
import android.os.Build
import android.os.UserHandleHidden
import android.util.Log
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.shared.utils.OsUtil
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.annotation.Anywhere
import top.xjunz.tasker.app
import top.xjunz.tasker.isAppProcess
import java.lang.reflect.Field


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
    fun getAppResourceContext(): Context {
        return if (isAppProcess) app else createAppContext()
    }

    private fun createAppContextCompat(
        ctx: Context,
        activityThread: ActivityThread,
        opPkgName: String?
    ): Context {
        val cls = Class.forName("android.app.ContextImpl")
        val field: Field = cls.getDeclaredField("mPackageInfo")
        field.isAccessible = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val method = cls.getDeclaredMethod(
                "createAppContext",
                ActivityThread::class.java, Class.forName("android.app.LoadedApk"),
                String::class.java
            )
            method.isAccessible = true
            return method.invoke(null, activityThread, field.get(ctx), opPkgName) as Context
        } else {
            val method = cls.getDeclaredMethod(
                "createAppContext",
                ActivityThread::class.java, Class.forName("android.app.LoadedApk")
            )
            method.isAccessible = true
            return method.invoke(null, activityThread, field.get(ctx)) as Context
        }
    }

    private fun createAppContext(): Context {
        val activityThread: ActivityThread = ActivityThread.currentActivityThread()
        val sysCtx: Context = activityThread.systemContext
        val opPkgName = if (OsUtil.isInShellProcess) SHELL_APPLICATION_ID else "android"
        return try {
            val ctx: Context = sysCtx.casted<ContextHidden>()
                .createPackageContextAsUser(BuildConfig.APPLICATION_ID, 0, UserHandleHidden.of(0))
            createAppContextCompat(ctx, activityThread, opPkgName)
        } catch (th: Throwable) {
            th.logcatStackTrace()
            sysCtx
        }
    }

    private fun createPrivilegedContext(): Context {
        val activityThread: ActivityThread = ActivityThread.currentActivityThread()
        val sysCtx: Context = activityThread.systemContext
        return if (OsUtil.isInRootProcess) {
            sysCtx
        } else try {
            val ctx: Context = sysCtx.casted<ContextHidden>().createPackageContextAsUser(
                SHELL_APPLICATION_ID, 0, UserHandleHidden.of(0)
            )
            createAppContextCompat(ctx, activityThread, SHELL_APPLICATION_ID)
        } catch (th: Throwable) {
            Log.e("Context", Log.getStackTraceString(th))
            sysCtx
        }
    }
}