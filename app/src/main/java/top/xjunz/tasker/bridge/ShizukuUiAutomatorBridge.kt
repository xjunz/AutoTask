package top.xjunz.tasker.bridge

import android.app.UiAutomation
import android.content.Context
import android.content.pm.IPackageManager
import android.os.Parcel
import android.view.Display
import android.view.ViewConfiguration
import androidx.core.os.ParcelCompat
import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.InteractionController
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper

/**
 * @author xjunz 2022/09/30
 */
class ShizukuUiAutomatorBridge(uiAutomation: UiAutomation) : UiAutomatorBridge(uiAutomation) {

    override fun getRotation(): Int {
        val data = SystemServiceHelper.obtainParcel(
            Context.WINDOW_SERVICE, "android.view.IWindowManager", "getDefaultDisplayRotation"
        )
        val reply = Parcel.obtain()
        try {
            Shizuku.transactRemote(data, reply, 0)
            reply.readException()
            return reply.readInt()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    override fun isScreenOn(): Boolean {
        val data = SystemServiceHelper.obtainParcel(
            Context.POWER_SERVICE, "android.os.IPowerManager", "isInteractive"
        )
        val reply = Parcel.obtain()
        try {
            Shizuku.transactRemote(data, reply, 0)
            reply.readException()
            return ParcelCompat.readBoolean(reply)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    override fun getDefaultDisplay(): Display {
        return DisplayManagerBridge.defaultDisplay
    }

    override fun getLauncherPackageName(): String? {
        return IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package"))
            ?.getHomeActivities(arrayListOf())?.packageName
    }

    override fun getScaledMinimumFlingVelocity(): Int {
        @Suppress("DEPRECATION")
        return ViewConfiguration.getMinimumFlingVelocity()
    }

    override fun getInteractionController(): InteractionController {
        return InteractionController(this)
    }

    override fun getGestureController(device: UiDevice): GestureController {
        return GestureController(device)
    }
}