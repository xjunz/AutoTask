package top.xjunz.tasker.task.inspector

import android.content.Context
import android.provider.Settings
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.task.factory.AppletOption

/**
 * @author xjunz 2021/9/21
 */
class FloatingInspector(baseContext: Context, val viewModel: InspectorViewModel) : LifecycleOwner {

    companion object {
        const val MODE_COMPONENT = 0
        const val MODE_UI_OBJECT = 1
        const val MODE_COORDINATE = 2

        fun isReady(): Boolean {
            return Settings.canDrawOverlays(app) && A11yAutomatorService.get() != null
        }

        fun require(): FloatingInspector {
            return A11yAutomatorService.require().inspector
        }
    }

    val context: Context = ContextThemeWrapper(baseContext, R.style.AppTheme)

    private val trashBin = TrashBinOverlay(this)

    private val collapsedBubble = CollapsedBubbleOverlay(this, trashBin)

    private val expandedBubble = ExpandedBubbleOverlay(this)

    private val toastOverlay = ToastOverlay(this)

    private val inspectorView = InspectorViewOverlay(this)

    private val nodeTree = NodeTreeOverlay(this)

    private val componentOverlay = ComponentOverlay(this)

    private val nodeInfo = NodeInfoOverlay(this)

    private val overlays: Array<BaseOverlay<*>> = arrayOf(
        trashBin,
        inspectorView,
        componentOverlay,
        collapsedBubble,
        expandedBubble,
        nodeTree,
        nodeInfo,
        toastOverlay
    )

    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    /**
     * Remove overlays to window manger.
     *
     * Do not call this directly outside of [A11yAutomatorService]. Use [A11yAutomatorService.destroyFloatingInspector].
     */
    fun dismiss() {
        overlays.forEach {
            it.removeFromWindowManager()
        }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    fun acceptUiObjectOptions(): List<AppletOption> {
        return nodeInfo.getCheckedOptions()
    }

    /**
     * Add overlays to window manger.
     *
     * Do not call this directly outside of [A11yAutomatorService]. Use [A11yAutomatorService.showFloatingInspector].
     */
    fun show() {
        check(!isShown) {
            "The inspector is already shown!"
        }

        overlays.forEach {
            it.init()
        }
        overlays.forEach {
            it.addToWindowManager()
        }
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    val isShown: Boolean get() = lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)
}
