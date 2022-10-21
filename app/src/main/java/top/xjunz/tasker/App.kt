package top.xjunz.tasker

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.os.Build
import androidx.appcompat.view.ContextThemeWrapper
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * @author xjunz 2021/6/25
 */
val app: App get() = App.INSTANCE!!

val isInHostProcess: Boolean get() = App.INSTANCE != null

val isInRemoteProcess: Boolean get() = !isInHostProcess

class App : Application() {

    companion object {

        var INSTANCE: App? = null

    }

    lateinit var appTheme: Theme
        private set

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        generateAppTheme()
    }

    private fun generateAppTheme() {
        appTheme = ContextThemeWrapper(this, R.style.AppTheme).theme
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        INSTANCE = this
        generateAppTheme()
    }

    fun sharedPrefsOf(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }
}

