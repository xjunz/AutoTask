package top.xjunz.tasker.task.flow

import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.system.Os
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.flow.If
import top.xjunz.tasker.isInHostProcess

/**
 * @author xjunz 2022/09/03
 */
@Serializable
@SerialName("PackageFlow")
class PackageFlow : If() {

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

    override fun onPrepare(ctx: AppletContext, runtime: FlowRuntime) {
        super.onPreApply(ctx, runtime)
        val info = ctx.getOrPutArgument(id) {
            getPackageInfo(ctx.currentPackageName)
        }
        runtime.setTarget(info)
    }

}