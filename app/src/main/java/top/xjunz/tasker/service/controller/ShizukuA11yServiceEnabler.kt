package top.xjunz.tasker.service.controller

import android.content.ComponentName
import android.os.IBinder
import android.os.IInterface
import rikka.shizuku.Shizuku
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.service.IAutomatorConnection
import top.xjunz.tasker.service.ShizukuAutomatorService

/**
 * @author xjunz 2022/10/10
 */
class ShizukuA11yServiceEnabler : ShizukuServiceController<IAutomatorConnection>() {

    override val tag: String = "ShizukuA11yEnabler"

    override var service: IAutomatorConnection? = null

    override val userServiceStandaloneProcessArgs: Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID, ShizukuAutomatorService::class.java.name
            )
        ).processNameSuffix("enabler").debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    override val startTimestamp: Long = -1

    override fun asInterface(binder: IBinder): IInterface {
        return IAutomatorConnection.Stub.asInterface(binder)
    }

    override fun doOnConnected(serviceInterface: IInterface) {
        service = serviceInterface as IAutomatorConnection
    }

    fun enableA11yService(exitOnFinally: Boolean) {
        doWhenRunning {
            try {
                it.executeShellCmd(
                    "settings put secure enabled_accessibility_services" +
                            " ${BuildConfig.APPLICATION_ID}/${A11yAutomatorService::class.java.name}"
                )
            } finally {
                if (exitOnFinally) {
                    it.destroy()
                }
            }
        }
    }


}