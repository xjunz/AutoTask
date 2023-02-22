/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service.controller

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.os.SharedMemory
import androidx.lifecycle.MutableLiveData
import rikka.shizuku.Shizuku
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.engine.dto.XTaskDTO.Serializer.toDTO
import top.xjunz.tasker.service.IRemoteAutomatorService
import top.xjunz.tasker.service.ShizukuAutomatorService
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.util.ShizukuUtil


/**
 * @author xjunz 2021/6/26
 */
object ShizukuAutomatorServiceController : ShizukuServiceController<ShizukuAutomatorService>() {

    override val tag = "ShizukuAutomatorService"

    private const val SERVICE_NAME_SUFFIX = "service"

    override var service: ShizukuAutomatorService? = null

    override val userServiceStandaloneProcessArgs: Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID, ShizukuAutomatorService::class.java.name
            )
        ).processNameSuffix(SERVICE_NAME_SUFFIX).debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    @SuppressLint("BlockedPrivateApi", "PrivateApi")
    override fun onServiceConnected(remote: IInterface) {
        remote as IRemoteAutomatorService
        service = ShizukuAutomatorService(remote)
        runCatching {
            if (!remote.isConnected) {
                remote.connect()
                if (Build.VERSION.SDK_INT >= 31) {
                    val field = Typeface::class.java.getDeclaredField("sSystemFontMapSharedMemory")
                    field.isAccessible = true
                    remote.setSystemTypefaceSharedMemory(field.get(null) as SharedMemory)
                }
            }
        }.onSuccess {
            val rtm = remote.taskManager
            LocalTaskManager.setRemotePeer(rtm)
            if (!rtm.isInitialized) {
                rtm.initialize(LocalTaskManager.getEnabledResidentTasks().map { it.toDTO() })
            }
        }.onFailure {
            remote.destroy()
            throw it
        }
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