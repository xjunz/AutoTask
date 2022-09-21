package top.xjunz.tasker.service

import androidx.test.uiautomator.mock.MockInstrumentation
import top.xjunz.tasker.impl.IAvailabilityChecker

/**
 * A service defines the common abstractions of [A11yAutomatorService] and [ShizukuAutomatorService].
 *
 * @author xjunz 2022/07/21
 */
interface AutomatorService : MockInstrumentation {

    val isRunning: Boolean

    fun destroy()

    fun getStartTimestamp(): Long

    fun createAvailabilityChecker(): IAvailabilityChecker
}