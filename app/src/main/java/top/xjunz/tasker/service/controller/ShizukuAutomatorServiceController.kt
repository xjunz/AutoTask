/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service.controller

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.Typeface
import android.os.*
import rikka.shizuku.Shizuku
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.engine.dto.toDTO
import top.xjunz.tasker.ktx.whenAlive
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.IRemoteAutomatorService
import top.xjunz.tasker.service.ShizukuAutomatorService
import top.xjunz.tasker.task.runtime.LocalTaskManager


/**
 * @author xjunz 2021/6/26
 */
object ShizukuAutomatorServiceController : ShizukuServiceController<ShizukuAutomatorService>() {

    override val tag = "ShizukuAutomatorService"

    private const val SERVICE_NAME_SUFFIX = "service"

    override var service: ShizukuAutomatorService? = null

    var remoteService: IRemoteAutomatorService? = null

    override val userServiceStandaloneProcessArgs: Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, ShizukuAutomatorService::class.java.name)
        ).processNameSuffix(SERVICE_NAME_SUFFIX).debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    @SuppressLint("BlockedPrivateApi", "PrivateApi")
    override fun onServiceConnected(remote: IInterface) {
        remote as IRemoteAutomatorService
        if (remote.isConnected) {
            doFinally(remote)
        } else {
            remote.connect(object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    if (resultCode < 0) {
                        remote.destroy()
                        listener?.onError(resultData?.getString(ShizukuAutomatorService.KEY_CONNECTION_ERROR))
                    } else {
                        remote.setPremiumContextStoragePath(PremiumMixin.premiumContextStoragePath)
                        remote.loadPremiumContext()
                        if (Build.VERSION.SDK_INT >= 31) {
                            val field =
                                Typeface::class.java.getDeclaredField("sSystemFontMapSharedMemory")
                            field.isAccessible = true
                            remote.setSystemTypefaceSharedMemory(field.get(null) as SharedMemory)
                        }
                        doFinally(remote)
                    }
                }
            })
        }
    }

    private fun doFinally(remote: IRemoteAutomatorService) {
        val rtm = remote.taskManager
        LocalTaskManager.setRemotePeer(rtm)
        if (!rtm.isInitialized) {
            rtm.initialize(LocalTaskManager.getEnabledResidentTasks().map { it.toDTO() })
        }
        remoteService = remote
        service = ShizukuAutomatorService(remote)
    }

    override fun stopService() {
        super.stopService()
        service = null
    }

    override fun asInterface(binder: IBinder): IRemoteAutomatorService {
        return IRemoteAutomatorService.Stub.asInterface(binder)
    }

    override val startTimestamp: Long
        get() {
            return service?.whenAlive {
                it.startTimestamp
            } ?: -1L
        }
}