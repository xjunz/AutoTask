/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.databinding.OverlayWindowBoundsDetectorBinding
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.ui.widget.GesturePlaybackView

/**
 * @author xjunz 2022/10/31
 */
class BoundsDetectorOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayWindowBoundsDetectorBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val gesturePlaybackView get() = rootView.casted<GesturePlaybackView>()

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        rootView.doOnPreDraw {
            vm.windowHeight = it.height
            vm.windowWidth = it.width
        }
        inspector.observeTransient(vm.playbackGesture) {
            gesturePlaybackView.setGesture(it)
        }
        inspector.observeTransient(vm.currentDuration) {
            gesturePlaybackView.updateCurrentDuration(it)
        }
        inspector.observeTransient(vm.onGesturePerformed) {
            gesturePlaybackView.clear()
        }
    }
}