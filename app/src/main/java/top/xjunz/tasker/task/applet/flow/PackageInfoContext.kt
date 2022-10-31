package top.xjunz.tasker.task.applet.flow

import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.system.Os
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.app
import top.xjunz.tasker.isInHostProcess

/**
 * @author xjunz 2022/10/02
 */
class PackageInfoContext(
    val packageName: String,
    val activityName: String?,
    val panelTitle: String?
) {

    val packageInfo: PackageInfo by lazy {
        getPackageInfo(packageName)
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return if (isInHostProcess) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getPackageInfo(
                    packageName, PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(packageName, 0)
            }
        } else {
            IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package"))
                .getPackageInfo(packageName, 0, Os.getuid())
        }
    }

}