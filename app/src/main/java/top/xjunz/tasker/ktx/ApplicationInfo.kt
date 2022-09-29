package top.xjunz.tasker.ktx

import android.content.pm.ApplicationInfo

/**
 * @author xjunz 2022/09/22
 */

val ApplicationInfo.isSystemApp
    get() = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0