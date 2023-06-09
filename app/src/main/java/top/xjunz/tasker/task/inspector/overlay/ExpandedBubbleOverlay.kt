/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayBubbleExpandedBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/10/17
 */
class ExpandedBubbleOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayBubbleExpandedBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.x = vm.bubbleX
        base.y = vm.bubbleY
    }

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.apply {
            ibBottom.asPointerController(KeyEvent.KEYCODE_DPAD_DOWN)
            ibLeft.asPointerController(KeyEvent.KEYCODE_DPAD_LEFT)
            ibRight.asPointerController(KeyEvent.KEYCODE_DPAD_RIGHT)
            ibTop.asPointerController(KeyEvent.KEYCODE_DPAD_UP)
            draggableRoot.setOnDragListener { _, offsetX, offsetY ->
                offsetViewInWindow(offsetX.toInt(), offsetY.toInt())
                vm.bubbleX = layoutParams.x
                vm.bubbleY = layoutParams.y
            }
            ibConfirm.setNoDoubleClickListener {
                when (vm.currentMode.require()) {
                    InspectorMode.UI_OBJECT -> vm.showNodeInfo.value = true
                    InspectorMode.COMPONENT -> vm.onComponentSelected.value = true
                    InspectorMode.COORDS -> vm.onCoordinateSelected.value = true
                    InspectorMode.GESTURE_RECORDER -> vm.showGestures.value = true
                    InspectorMode.TASK_ASSISTANT -> {
                        /* no-op */
                    }
                }
            }
            ibLayers.setNoDoubleClickListener {
                vm.showNodeTree.value = true
            }
            ibCenter.setNoDoubleClickListener {
                vm.showGamePad.value = false
            }
            ibCollapse.setNoDoubleClickListener {
                vm.isCollapsed.toggle()
            }
            ibGamePad.setNoDoubleClickListener {
                vm.showGamePad.value = true
                vm.makeToast(R.string.pointer_controller_enabled)
            }
            ibPinScreenshot.setNoDoubleClickListener {
                vm.pinScreenShot.toggle()
                if (vm.pinScreenShot.isTrue) {
                    vm.makeToast(R.string.pin_screenshot)
                } else {
                    vm.makeToast(R.string.cancel_pin_screenshot)
                }
            }
            ibShowGrid.setNoDoubleClickListener {
                vm.showGrids.toggle()
                if (vm.showGrids.isTrue) {
                    vm.makeToast(R.string.show_node_bounds)
                } else {
                    vm.makeToast(R.string.cancel_show_node_bounds)
                }
            }
            inspector.observe(vm.isCollapsed) {
                if (!it) {
                    layoutParams.x = vm.bubbleX
                    layoutParams.y = vm.bubbleY
                    if (root.isAttachedToWindow) windowManager.updateViewLayout(root, layoutParams)
                }
                root.isVisible = !it
            }
            inspector.observe(vm.showGamePad) {
                gamePad.isVisible = it
                ibGamePad.isEnabled = !it
            }
            inspector.observe(vm.pinScreenShot) {
                if (it) {
                    ibPinScreenshot.setImageResource(R.drawable.ic_outline_image_24)
                } else {
                    ibPinScreenshot.setImageResource(R.drawable.ic_outline_hide_image_24)
                }
            }
            inspector.observe(vm.showGrids) {
                if (it) {
                    ibShowGrid.setImageResource(R.drawable.ic_baseline_grid_on_24)
                } else {
                    ibShowGrid.setImageResource(R.drawable.ic_baseline_grid_off_24)
                }
            }
            inspector.observe(vm.isConfirmButtonEnabled) {
                ibConfirm.isEnabled = it
            }
            inspector.observeNotNull(vm.currentMode) {
                ibPinScreenshot.isVisible = false
                ibLayers.isVisible = false
                ibGamePad.isVisible = false
                ibShowGrid.isVisible = false
                ibRecordAction.isVisible = false
                cvToolbar.isVisible = true
                when (it) {
                    InspectorMode.UI_OBJECT -> {
                        ibPinScreenshot.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ibLayers.isVisible = true
                        ibGamePad.isVisible = true
                        ibShowGrid.isVisible = true
                    }
                    InspectorMode.COMPONENT -> {
                        /* no-op */
                    }
                    InspectorMode.COORDS -> ibGamePad.isVisible = true
                    InspectorMode.GESTURE_RECORDER -> ibRecordAction.isVisible = true
                    InspectorMode.TASK_ASSISTANT -> cvToolbar.isVisible = false
                }
            }
            ibRecordAction.setNoDoubleClickListener {
                if (vm.isRecordingGesture.isTrue) {
                    if (vm.recordedEvents.require().isEmpty()) {
                        vm.makeToast(R.string.error_no_gesture_recorded)
                    } else {
                        vm.requestRecordingState.value = false
                        vm.makeToast(R.string.gesture_record_finished)
                    }
                } else {
                    vm.clearAllRecordedEvents()
                    vm.requestRecordingState.value = true
                    vm.makeToast(R.string.start_recording_gestures)
                }
            }
            inspector.observe(vm.isRecordingGesture) {
                if (it) {
                    ibConfirm.isEnabled = false
                    ibRecordAction.setImageResource(R.drawable.ic_baseline_stop_24)
                    ibRecordAction.setContentDescriptionAndTooltip(R.string.stop.str)
                } else {
                    ibConfirm.isEnabled = true
                    ibRecordAction.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    ibRecordAction.setContentDescriptionAndTooltip(R.string.record_gesture.str)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun View.asPointerController(keyCode: Int) {
        setOnLongClickListener l@{
            vm.onKeyLongPressed.value = keyCode
            return@l keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT
        }
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                vm.onKeyUpOrCancelled.value = keyCode
            }
            return@setOnTouchListener false
        }
    }
}