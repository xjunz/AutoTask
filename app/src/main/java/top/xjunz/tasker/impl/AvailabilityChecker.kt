package top.xjunz.tasker.impl

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.service.AutomatorService
import java.util.concurrent.CompletableFuture

/**
 * @author xjunz 2022/07/27
 */
class AvailabilityChecker(
    private val service: AutomatorService,
    private val looper: Looper? = null
) : IAvailabilityChecker.Stub() {

    private val buttonId = "btn_target"

    private val dropTargetId = "view_drop_target"

    private val uiDevice by lazy {
        UiDevice.getInstance(service)
    }

    private val handler by lazy {
        Handler(looper!!)
    }

    override fun launchCheck(caseName: Int, listener: IAvailabilityCheckerCallback) {
        if (looper == null) {
            doCheck(caseName, listener)
        } else {
            handler.removeCallbacksAndMessages(null)
            val future = CompletableFuture<Throwable>()
            handler.post {
                try {
                    doCheck(caseName, listener)
                    future.complete(null)
                } catch (t: Throwable) {
                    future.complete(t)
                }
            }
            val t = future.get()
            if (t != null) throw t
        }
    }


    private fun doCheck(caseName: Int, listener: IAvailabilityCheckerCallback) {
        when (caseName) {
            R.string.case_widget_recognition -> {
                listener.onCompleted(
                    uiDevice.findObject(By.res(BuildConfig.APPLICATION_ID, buttonId)).text
                )
            }
            R.string.case_global_action -> {
                service.interactionController.toggleRecentApps()
                SystemClock.sleep(1000)
                service.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                SystemClock.sleep(1000)
                service.interactionController.openNotification()
                SystemClock.sleep(1000)
                service.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                listener.onCompleted(null)
            }
            R.string.case_drag_and_drop -> {
                val dropTarget = checkNotNull(
                    uiDevice.findObject(By.res(BuildConfig.APPLICATION_ID, dropTargetId))
                ) {
                    "Cannot find the drop target!"
                }
                val dropCenter = dropTarget.visibleCenter
                val dragTarget = checkNotNull(
                    uiDevice.findObject(By.res(BuildConfig.APPLICATION_ID, buttonId))
                ) {
                    "Cannot find the drag target!"
                }
                dragTarget.drag(dropCenter, 1000)
                listener.onCompleted(null)
            }
        }
    }

}