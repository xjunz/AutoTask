package top.xjunz.tasker.service

import androidx.annotation.StringRes
import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.service.controller.A11yAutomatorServiceController
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.controller.ShizukuAutomatorServiceController

/**
 * @author xjunz 2022/07/23
 */
sealed class OperatingMode(
    val VALUE: Int,
    @StringRes private val nameRes: Int,
    @StringRes private val descRes: Int
) {

    val name get() = nameRes.str

    val description get() = descRes.str

    abstract val serviceController: ServiceController<out AutomatorService>

    object Shizuku : OperatingMode(0, R.string.shizuku, R.string.desc_shizuku_mode) {
        override val serviceController = ShizukuAutomatorServiceController
    }

    object Accessibility : OperatingMode(1, R.string.a11y_service, R.string.desc_a11y_mode) {
        override val serviceController = A11yAutomatorServiceController
    }

    companion object {

        val CURRENT get() = fromValue(Preferences.operatingMode)

        private fun fromValue(value: Int): OperatingMode {
            if (value == Shizuku.VALUE) return Shizuku
            if (value == Accessibility.VALUE) return Accessibility
            runtimeException("Unknown operating mode: $value")
        }
    }
}