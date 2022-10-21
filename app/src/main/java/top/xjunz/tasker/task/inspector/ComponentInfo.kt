package top.xjunz.tasker.task.inspector

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import top.xjunz.tasker.app

/**
 * @author xjunz 2022/10/18
 */
data class ComponentInfo(val activityLabel: String?, val pkgName: String, val actName: String) {

    fun isActivity(): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getActivityInfo(
                    ComponentName(pkgName, actName),
                    PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getActivityInfo(ComponentName(pkgName, actName), 0)
            }
        }.isSuccess
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentInfo) return false

        if (activityLabel != other.activityLabel) return false
        if (pkgName != other.pkgName) return false
        if (actName != other.actName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activityLabel?.hashCode() ?: 0
        result = 31 * result + pkgName.hashCode()
        result = 31 * result + actName.hashCode()
        return result
    }
}