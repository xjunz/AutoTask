package top.xjunz.tasker.util

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.launchIntentSafely
import top.xjunz.tasker.ktx.toast

/**
 * @author xjunz 2022/07/24
 */
object ShizukuUtil {

    inline val isShizukuAvailable
        get() = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    inline val isShizukuInstalled
        get() = app.packageManager.getLaunchIntentForPackage(MANAGER_APPLICATION_ID) != null

    fun launchShizukuManager() {
        val launchIntent = app.packageManager.getLaunchIntentForPackage(MANAGER_APPLICATION_ID)
        if (launchIntent == null) {
            toast(R.string.shizuku_not_installed)
        } else {
            app.launchIntentSafely(launchIntent)
        }
    }
}