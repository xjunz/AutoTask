package top.xjunz.tasker.service.controller

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Point
import android.os.IBinder
import android.util.Log
import android.view.ViewConfiguration
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.IAutomatorConnection
import top.xjunz.tasker.app
import top.xjunz.tasker.impl.MockContextAdaptor
import top.xjunz.tasker.service.AutomatorService
import top.xjunz.tasker.service.ShizukuAutomatorService
import top.xjunz.tasker.util.ShizukuUtil
import top.xjunz.tasker.util.runtimeException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException


/**
 * @author xjunz 2021/6/26
 */
object ShizukuServiceController : ServiceController() {

    private const val tag = "Automator"

    private const val BINDING_SERVICE_TIMEOUT_MILLS = 5000L

    private const val SERVICE_NAME_SUFFIX = "service"

    private var binder: IBinder? = null

    private val userServiceStandaloneProcessArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID, ShizukuAutomatorService::class.java.name
            )
        ).processNameSuffix(SERVICE_NAME_SUFFIX).debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val deathRecipient: IBinder.DeathRecipient by lazy {
        IBinder.DeathRecipient {
            Log.w(tag, "The remote service is dead!")
            listener?.onServiceDisconnected()
            binder?.unlinkToDeath(deathRecipient, 0)
        }
    }

    private var bindingJob: Job? = null

    private val userServiceConnection = object : ServiceConnection {

        private fun initAutomatorContext(connection: IAutomatorConnection) {
            val adaptor = MockContextAdaptor(app)
            val realSize = Point()
            val size = Point()
            adaptor.getRealSize(realSize)
            adaptor.getSize(size)
            connection.initAutomatorContext(
                realSize, size, adaptor.density,
                ViewConfiguration.get(app).scaledMinimumFlingVelocity
            )
        }

        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            try {
                if (iBinder == null || !iBinder.pingBinder()) {
                    runtimeException("Got an invalid binder!")
                } else {
                    iBinder.linkToDeath(deathRecipient, 0)
                    IAutomatorConnection.Stub.asInterface(iBinder).also {
                        service = ShizukuAutomatorService(it)
                        if (!it.isConnected) {
                            it.connect()
                            initAutomatorContext(it)
                        }
                    }
                    binder = iBinder
                    listener?.onServiceBound()
                }
            } catch (t: Throwable) {
                listener?.onError(t)
                service?.destroy()
            } finally {
                bindingJob?.cancel()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    val isShizukuInstalled = MutableLiveData<Boolean>()

    val isShizukuGranted = MutableLiveData<Boolean>()

    override var service: AutomatorService? = null

    override fun bindService() {
        listener?.onStartBinding()
        bindingJob = async {
            Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            delay(BINDING_SERVICE_TIMEOUT_MILLS)
            throw TimeoutException()
        }
        bindingJob?.invokeOnCompletion {
            if (it != null && it !is CancellationException) {
                listener?.onError(it)
            }
            bindingJob = null
        }
    }

    override fun stopService() {
        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, true)
        service = null
    }

    override fun unbindService() {
        currentServiceController.removeStateListener()
        binder?.unlinkToDeath(deathRecipient, 0)
        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, false)
    }

    override fun bindExistingServiceIfExists() {
        if (ShizukuUtil.isShizukuAvailable &&
            Shizuku.peekUserService(userServiceStandaloneProcessArgs, userServiceConnection)
        ) {
            bindService()
        }
    }

    override fun syncStatus() {
        isShizukuInstalled.value = ShizukuUtil.isShizukuInstalled
        isShizukuGranted.value = ShizukuUtil.isShizukuAvailable
    }
}