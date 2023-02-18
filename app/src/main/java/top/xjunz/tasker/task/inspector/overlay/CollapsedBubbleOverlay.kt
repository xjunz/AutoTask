/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.view.WindowManager
import androidx.core.view.isVisible
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayBubbleCollapsedBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.main.EventCenter
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener
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
                        rootView.animate().alpha(0F).doWhenEnd {
                            a11yAutomatorService.destroyFloatingInspector()
                        }.start()
                    }
                    trashBinOverlay.fadeOut()
                }
            }
        }
        binding.ibCenter.setAntiMoneyClickListener {
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
            return@setOnLongClickListener true
        }
        inspector.observe(vm.isCollapsed) {
            if (it) {
                layoutParams.x = vm.bubbleX
                layoutParams.y = vm.bubbleY
                if (rootView.isAttachedToWindow)
                    windowManager.updateViewLayout(rootView, layoutParams)
            }
            rootView.isVisible = it
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