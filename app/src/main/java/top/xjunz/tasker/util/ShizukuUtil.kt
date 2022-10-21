package top.xjunz.tasker.util

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.launchIntentSafely
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ktx.viewUrlSafely
import kotlin.random.Random

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

    fun ensureShizukuEnv(onPrepared: () -> Unit) {
        if (isShizukuAvailable) {
            // Shizuku is prepared!
            onPrepared()
        } else if (Shizuku.pingBinder()) {
            // Shizuku service is started but we do not have the permission
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                // and we should show rationale
                toast(R.string.pls_grant_manually)
                launchShizukuManager()
            } else {
                // just request permission
                val code = Random.nextInt()
                val listener = object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                        if (requestCode == code) {
                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                onPrepared()
                            } else {
                                toast(R.string.grant_failed)
                            }
                        }
                        Shizuku.removeRequestPermissionResultListener(this)
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                Shizuku.requestPermission(code)
            }
        } else if (isShizukuInstalled) {
            // Shizuku service is not started while Shizuku manager is installed
            toast(R.string.prompt_start_shizuku_service)
            launchShizukuManager()
        } else {
            // Shizuku manager is not even installed
            toast(R.string.prompt_install_shizuku)
            app.viewUrlSafely("https://www.coolapk.com/apk/moe.shizuku.privileged.api")
        }
    }
}