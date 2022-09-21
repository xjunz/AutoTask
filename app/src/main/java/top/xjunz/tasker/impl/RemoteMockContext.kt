package top.xjunz.tasker.impl

import android.content.Context
import android.content.pm.IPackageManager
import android.graphics.Point
import android.os.Parcel
import androidx.core.os.ParcelCompat
import androidx.test.uiautomator.mock.MockContext
import androidx.test.uiautomator.mock.MockDisplay
import androidx.test.uiautomator.mock.MockDisplayMetrics
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper

/**
 * @author xjunz 2022/07/24
 */
class RemoteMockContext(private val realSize: Point, private val size: Point, density: Float) :
    MockContext, MockDisplay, MockDisplayMetrics(density) {

    override fun isInteractive(): Boolean {
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

    override fun getLauncherPackageName(): String? {
        return IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package"))
            ?.getHomeActivities(arrayListOf())?.packageName
    }

    override fun getDefaultDisplay(): MockDisplay {
        return this
    }

    override fun getDisplayMetrics(): MockDisplayMetrics {
        return this
    }

    override fun getRealSize(p: Point) {
        p.set(realSize.x, realSize.y)
    }

    override fun getRealMetrics(): MockDisplayMetrics {
        return this
    }

    override fun getSize(p: Point) {
        p.set(size.x, size.y)
    }

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
}