/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources.Theme
import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.api.UpdateInfo
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.serviceController
import java.io.File
import java.lang.ref.WeakReference
import kotlin.system.exitProcess


/**
 * @author xjunz 2021/6/25
 */
val isAppProcess: Boolean get() = App.instance != null

val isPrivilegedProcess: Boolean get() = !isAppProcess

val app: App get() = requireNotNull(App.instance)

const val upForGrabs = true

class App : Application() {

    var updateInfo = MutableLiveData<UpdateInfo>()

    private lateinit var appThemeRef: WeakReference<Theme>

    val appTheme get() = requireNotNull(appThemeRef.get())

    companion object {

        var instance: App? = null
            private set

        private const val SHARED_FILE_CACHE_SUBDIR_NAME = "share"
    }

    fun setAppTheme(theme: Theme) {
        appThemeRef = WeakReference(theme)
    }

    fun getSharedFileCacheDir(): File {
        return File(cacheDir, SHARED_FILE_CACHE_SUBDIR_NAME)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            logcat(e.stackTraceToString(), Log.ERROR)
            if (serviceController.isServiceRunning) {
                runCatching {
                    currentService.destroy()
                }.onFailure {
                    logcat(e.stackTraceToString(), Log.ERROR)
                }
            }
            exitProcess(-1)
        }
        Sui.init(BuildConfig.APPLICATION_ID)
        if (!BuildConfig.DEBUG) {
            AppCenter.start(
                this, "5cc80607-5168-4c05-b3d6-acbbfc25f8df",
                Analytics::class.java, Crashes::class.java
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        PremiumMixin.premiumContextStoragePath =
            File(getExternalFilesDir(""), PremiumMixin.PREMIUM_CONTEXT_FILE_NAME).path
        PremiumMixin.loadPremiumFromFileSafely()
    }

    fun sharedPrefsOf(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }
}

