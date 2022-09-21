package top.xjunz.tasker.task.core

import android.provider.Settings
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.ui.task.inspector.FloatingInspector

/**
 * @author xjunz 2022/07/31
 */
object InspectorController {

    private lateinit var floatingInspector: FloatingInspector

    fun showInspector() {
        if (!::floatingInspector.isInitialized) {
            floatingInspector = FloatingInspector()
        }
        if (floatingInspector.isShown) return
        floatingInspector.show()
    }

    fun dismissInspector() {
        if (::floatingInspector.isInitialized && floatingInspector.isShown) {
            floatingInspector.dismiss()
        }
    }

    fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(app)
    }

    fun canRetrieveWindowRoot(): Boolean {
        return A11yAutomatorService.RUNNING.isTrue
    }

    fun isReady(): Boolean {
        return canDrawOverlay() && canRetrieveWindowRoot()
    }
}