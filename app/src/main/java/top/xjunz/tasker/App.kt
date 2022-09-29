package top.xjunz.tasker

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.xjunz.tasker.ui.ColorSchemes
import java.lang.ref.WeakReference

/**
 * @author xjunz 2021/6/25
 */

val app: App get() = App.instance!!

val appTheme: Resources.Theme get() = App.appThemeRef.get()!!

val colorSchemes: ColorSchemes get() = App.colorSchemesRef.get()!!

val isInHostProcess: Boolean get() = App.instance != null

val isInRemoteProcess: Boolean get() = !isInHostProcess

class App : Application() {

    companion object {

        var instance: App? = null

        lateinit var appThemeRef: WeakReference<Resources.Theme>

        lateinit var colorSchemesRef: WeakReference<ColorSchemes>
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

    fun sharedPrefsOf(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }
}

