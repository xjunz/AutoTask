/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.task.inspector.overlay.*

/**
 * @author xjunz 2021/9/21
 */
class FloatingInspector(baseContext: Context, val viewModel: InspectorViewModel) : LifecycleOwner {

    companion object {

        const val EVENT_COORDINATE_SELECTED = "inspector.event.COORDINATE_SELECTED"
        const val EVENT_COMPONENT_SELECTED = "inspector.event.COMPONENT_SELECTED"
        const val EVENT_NODE_INFO_SELECTED = "inspector.event.NODE_INFO_SELECTED"
        const val EVENT_GESTURES_RECORDED = "inspector.event.GESTURES_RECORDED"
        const val EVENT_REQUEST_EDIT_GESTURES = "inspector.event.REQUEST_EDIT_GESTURES"

        fun isReady(): Boolean {
            return Settings.canDrawOverlays(app) && A11yAutomatorService.get() != null
        }
    }

    val windowManager: WindowManager by lazy {
        baseContext.getSystemService(WindowManager::class.java)
    }

    val context: Context = ContextThemeWrapper(baseContext, R.style.AppTheme)

    private val trashBin = TrashBinOverlay(this)

    private val nodeInfo = NodeInfoOverlay(this)

    private val overlays: Array<FloatingInspectorOverlay<*>> = arrayOf(
        BoundsDetectorOverlay(this),
        trashBin,
        InspectorViewOverlay(this),
        ComponentOverlay(this),
        CollapsedBubbleOverlay(this, trashBin),
        ExpandedBubbleOverlay(this),
        TaskAssistantOverlay(this),
        NodeTreeOverlay(this),
        nodeInfo,
        GestureShowcaseOverlay(this),
        ToastOverlay(this),
    )

    private val lifecycleRegistry = LifecycleRegistry(this)

    val exemptionEventClassName: String by lazy {
        "FloatingInspectorWindow@" + hashCode()
    }

    val mode: InspectorMode
        get() = viewModel.currentMode.require()

    /**
     * Remove overlays to window manger.
     *
     * Do not call this directly outside of [A11yAutomatorService].
     * Use [A11yAutomatorService.destroyFloatingInspector].
     */
    fun dismiss() {
        overlays.forEach {
            it.removeFromWindowManager()
        }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    /**
     * Add overlays to window manger.
     *
     * Do not call this directly outside of [A11yAutomatorService].
     * Use [A11yAutomatorService.showFloatingInspector].
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

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
