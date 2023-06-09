/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.view.WindowManager
import androidx.core.view.isVisible
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayBubbleCollapsedBinding
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.isNull
import top.xjunz.tasker.ktx.observeNostalgic
import top.xjunz.tasker.ktx.toggle
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.main.EventCenter
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.Icons

/**
 * @author xjunz 2022/10/17
 */
class CollapsedBubbleOverlay(
    inspector: FloatingInspector, private val trashBinOverlay: TrashBinOverlay,
) : FloatingInspectorOverlay<OverlayBubbleCollapsedBinding>(inspector) {

    private fun desaturate() {
        rootView.background.colorFilter = Icons.desaturatedColorFilter
        binding.ibCenter.colorFilter = Icons.desaturatedColorFilter
    }

    private fun saturate() {
        rootView.background.colorFilter = null
        binding.ibCenter.colorFilter = null
    }

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.x = vm.bubbleX
        base.y = vm.bubbleY
    }

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.bubbleWrapper.setOnDragListener { state, offsetX, offsetY ->
            when (state) {
                FloatingDraggableLayout.STATE_DRAG_STARTED -> {
                    trashBinOverlay.layout()
                    trashBinOverlay.fadeIn()
                }
                FloatingDraggableLayout.STATE_DRAGGING -> {
                    offsetViewInWindow(offsetX.toInt(), offsetY.toInt())
                    vm.bubbleX = layoutParams.x
                    vm.bubbleY = layoutParams.y
                    if (trashBinOverlay.isInside(this)) {
                        desaturate()
                    } else {
                        saturate()
                    }
                }
                FloatingDraggableLayout.STATE_DRAG_ENDED -> {
                    if (trashBinOverlay.isInside(this)) {
                        rootView.animate().alpha(0F).withEndAction {
                            a11yAutomatorService.destroyFloatingInspector()
                        }.start()
                    }
                    trashBinOverlay.fadeOut()
                }
            }
        }
        binding.ibCenter.setNoDoubleClickListener {
            if (vm.currentMode eq InspectorMode.COMPONENT) {
                if (vm.currentComponent.isNull()) {
                    vm.makeToast(R.string.error_no_comp_detected)
                } else {
                    vm.onComponentSelected.value = true
                }
            } else {
                vm.isCollapsed.toggle()
            }
        }
        binding.ibCenter.setOnLongClickListener {
            EventCenter.launchHost()
            Preferences.showLongClickToHost = false
            return@setOnLongClickListener true
        }
        inspector.observeNostalgic(vm.isCollapsed) { prev, isCollapsed ->
            if (Preferences.showLongClickToHost && prev == false && isCollapsed) {
                vm.makeToast(R.string.tip_long_click_to_route_host)
            }
            if (isCollapsed) {
                layoutParams.x = vm.bubbleX
                layoutParams.y = vm.bubbleY
                if (rootView.isAttachedToWindow) {
                    windowManager.updateViewLayout(rootView, layoutParams)
                }
            }
            rootView.isVisible = isCollapsed
        }
    }

    override fun onDismiss() {
        super.onDismiss()
        saturate()
        layoutParams.x = 0
        layoutParams.y = 0
        rootView.isVisible = false
    }

}