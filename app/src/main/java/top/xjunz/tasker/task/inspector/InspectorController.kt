package top.xjunz.tasker.task.inspector

import android.provider.Settings
import top.xjunz.tasker.app
import top.xjunz.tasker.service.controller.A11yAutomatorServiceController

/**
 * @author xjunz 2022/07/31
 */
object InspectorController {

    private lateinit var floatingInspector: FloatingInspector

    fun showInspector() {
        if (!InspectorController::floatingInspector.isInitialized) {
            floatingInspector = FloatingInspector()
        }
        if (floatingInspector.isShown) return
        floatingInspector.show()
    }

    fun dismissInspector() {
        if (InspectorController::floatingInspector.isInitialized && floatingInspector.isShown) {
            floatingInspector.dismiss()
        }
    }

    fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(app)
    }

    fun canRetrieveWindowRoot(): Boolean {
        return A11yAutomatorServiceController.isServiceRunning
    }

    fun isReady(): Boolean {
        return canDrawOverlay() && canRetrieveWindowRoot()
    }

}