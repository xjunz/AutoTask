/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources.Theme
import android.os.Build
import androidx.lifecycle.MutableLiveData
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.xjunz.tasker.api.UpdateInfo
import top.xjunz.tasker.premium.PremiumMixin
import java.io.File
import java.lang.ref.WeakReference


/**
 * @author xjunz 2021/6/25
 */
val isAppProcess: Boolean get() = App.instance != null

val isPrivilegedProcess: Boolean get() = !isAppProcess

val app: App get() = requireNotNull(App.instance)

const val isShell = true

class App : Application() {

    var updateInfo = MutableLiveData<UpdateInfo>()

    private lateinit var appThemeRef: WeakReference<Theme>

    val appTheme get() = requireNotNull(appThemeRef.get())

    companion object {

        var instance: App? = null
            private set
    }

    fun setAppTheme(theme: Theme) {
        appThemeRef = WeakReference(theme)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppCenter.start(
            this, "5cc80607-5168-4c05-b3d6-acbbfc25f8df",
            Analytics::class.java, Crashes::class.java
        )
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

