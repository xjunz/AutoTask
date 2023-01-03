/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.privileged

import android.content.Context
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.ktx.execShellCmd

/**
 * @author xjunz 2022/11/15
 */
object ActivityManagerUtil {

    fun forceStopPackage(pkgName: String) {
        SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
            .execShellCmd("force-stop", pkgName)
    }
}