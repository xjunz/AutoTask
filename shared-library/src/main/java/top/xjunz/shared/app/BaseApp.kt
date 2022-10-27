package top.xjunz.shared.app

import android.app.Application

/**
 * @author xjunz 2022/10/27
 */
val app: Application get() = requireNotNull(BaseApp.instance)

open class BaseApp : Application() {

    companion object {

        var instance: BaseApp? = null
            private set

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}