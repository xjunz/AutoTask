package top.xjunz.tasker.service.controller

import android.content.ComponentName
import android.os.IBinder
import android.os.IInterface
import androidx.lifecycle.MutableLiveData
import rikka.shizuku.Shizuku
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.service.IRemoteAutomatorService
import top.xjunz.tasker.service.ShizukuAutomatorService
import top.xjunz.tasker.util.ShizukuUtil


/**
 * @author xjunz 2021/6/26
 */
object ShizukuAutomatorServiceController : ShizukuServiceController<ShizukuAutomatorService>() {

    override val tag = " ShizukuAutomatorService"

    private const val SERVICE_NAME_SUFFIX = "service"

    override var service: ShizukuAutomatorService? = null

    override val userServiceStandaloneProcessArgs: Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID, ShizukuAutomatorService::class.java.name
            )
        ).processNameSuffix(SERVICE_NAME_SUFFIX).debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    override fun doOnConnected(serviceInterface: IInterface) {
        serviceInterface as IRemoteAutomatorService
        service = ShizukuAutomatorService(serviceInterface)
        if (serviceInterface.isConnected) serviceInterface.connect()
    }

    val isShizukuInstalled = MutableLiveData<Boolean>()

    val isShizukuGranted = MutableLiveData<Boolean>()

    override fun stopService() {
        super.stopService()
        service = null
    }

    override fun syncStatus() {
        isShizukuInstalled.value = ShizukuUtil.isShizukuInstalled
        isShizukuGranted.value = ShizukuUtil.isShizukuAvailable
    }

    override fun asInterface(binder: IBinder): IRemoteAutomatorService {
        return IRemoteAutomatorService.Stub.asInterface(binder)
    }

    override val startTimestamp: Long = doWhenRunning { it.startTimestamp } ?: -1L
}