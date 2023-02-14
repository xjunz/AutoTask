/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import top.xjunz.tasker.databinding.OverlayComponentBinding
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout

/**
 * @author xjunz 2022/10/18
 */
class ComponentOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayComponentBinding>(inspector) {

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.draggableRoot.setOnDragListener { state: Int, offsetX: Float, offsetY: Float ->
            if (state == FloatingDraggableLayout.STATE_DRAGGING) {
                offsetViewInWindow(offsetX.toInt(), offsetY.toInt())
            }
        }
        rootView.doOnPreDraw {
            layoutParams.y = it.height / 2 - vm.windowHeight / 2
            updateViewLayout()
        }
        inspector.observe(vm.currentComp) {
            binding.tvTitle.text = it.paneTitle
            if (it.paneTitle != null) {
                binding.tvTitle.append("\n")
            }
            binding.tvTitle.append(it.packageName)
            if (it.activityName != null) {
                binding.tvTitle.append("\n")
                binding.tvTitle.append(it.activityName)
            }
        }
        inspector.observe(vm.currentMode) {
            rootView.isVisible = it == InspectorMode.COMPONENT
        }
    }
}