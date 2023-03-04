/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.os.Build
import androidx.appcompat.view.ContextThemeWrapper
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.xjunz.tasker.premium.PremiumMixin
import java.io.File

/**
 * @author xjunz 2021/6/25
 */
val isAppProcess: Boolean get() = App.instance != null

val isPrivilegedProcess: Boolean get() = !isAppProcess

val app: App get() = requireNotNull(App.instance)

class App : Application() {

    lateinit var appTheme: Theme
        private set

    companion object {

        var instance: App? = null
            private set

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        generateAppTheme()
    }

    private fun generateAppTheme() {
        appTheme = ContextThemeWrapper(this, R.style.AppTheme).theme
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        generateAppTheme()
        PremiumMixin.premiumContextStoragePath =
            File(getExternalFilesDir(""), PremiumMixin.PREMIUM_CONTEXT_FILE_NAME).path
        PremiumMixin.loadPremiumFromFileSafely()
    }

    fun sharedPrefsOf(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }
}

