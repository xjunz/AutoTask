package top.xjunz.tasker.bridge

import android.os.BatteryManager
import android.os.BatteryManagerHidden
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.app
import top.xjunz.tasker.isInHostProcess

/**
 * @author xjunz 2022/11/10
 */
object BatteryManagerBridge {

    private val batteryManager: BatteryManager by lazy {
        if (isInHostProcess) {
            app.getSystemService(BatteryManager::class.java)
        } else {
            BatteryManagerHidden().casted()
        }
    }

    val isCharging: Boolean get()  = batteryManager.isCharging

    val capacity: Int
        get() {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
}