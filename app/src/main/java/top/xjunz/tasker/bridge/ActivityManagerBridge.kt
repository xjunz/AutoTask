package top.xjunz.tasker.bridge

import android.app.ActivityManagerNative
import android.app.IActivityManager
import android.content.Context
import android.os.Build
import android.system.Os
import rikka.shizuku.SystemServiceHelper

/**
 * @author xjunz 2022/11/15
 */
object ActivityManagerBridge {

    fun forceStopPackage(pkgName: String) {
        val binder = SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
        if (Build.VERSION.SDK_INT >= 26) {
            IActivityManager.Stub.asInterface(binder)
        } else {
            ActivityManagerNative.asInterface(binder)
        }.forceStopPackage(pkgName, Os.getuid())
    }
}