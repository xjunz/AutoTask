package top.xjunz.tasker.bridge

import android.content.Context
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.ktx.execShellCmd

/**
 * @author xjunz 2022/11/15
 */
object ActivityManagerBridge {

    fun forceStopPackage(pkgName: String) {
        SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
            .execShellCmd("force-stop", pkgName)
    }
}