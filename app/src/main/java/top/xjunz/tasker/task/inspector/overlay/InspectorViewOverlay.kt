/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.lifecycle.lifecycleScope
import androidx.test.uiautomator.PointerGesture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayInspectorBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.task.inspector.StableNodeInfo
import top.xjunz.tasker.task.inspector.StableNodeInfo.Companion.freeze
import top.xjunz.tasker.uiautomator.A11yGestureController
import top.xjunz.tasker.uiautomator.GestureRecorder
import top.xjunz.tasker.util.Router.launchAction
import top.xjunz.tasker.util.Router.launchRoute

/**
 * @author xjunz 2022/10/16
 */
class InspectorViewOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayInspectorBinding>(inspector), GestureRecorder.Callback {

    companion object {
        const val DELAY_PERFORM_GESTURE = 50
    }

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val gestureRecorder = GestureRecorder(Looper.getMainLooper())

    private var screenshot: Bitmap? = null

    private var windowNode: StableNodeInfo? = null

    override fun onOverlayInflated() {
        gestureRecorder.setCallback(this)
        binding.apply {
            gestureRecorderView.setRecorder(gestureRecorder)
            inspectorView.setOnNodeClickListener {
                vm.highlightNode.setValueIfDistinct(it)
            }
            inspectorView.setOnNodeSelectedListener {
                vm.highlightNode.setValueIfDistinct(it)
            }
            inspector.observeTransient(vm.onKeyLongPressed) {
                inspectorView.onKeyLongPress(it, null)
            }
            inspector.observeTransient(vm.onKeyUpOrCancelled) {
                inspectorView.onKeyUp(it, null)
            }
            inspector.observe(vm.pinScreenShot) {
                if (it && screenshot != null) {
                    inspectorView.background = BitmapDrawable(context.resources, screenshot)
                } else {
                    inspectorView.background = null
                }
            }
            inspector.observe(vm.showGrids) {
                if (!inspectorView.hasNodeInfo() && windowNode != null) {
                    inspectorView.setRootNode(windowNode!!)
                    vm.currentNodeTree.value = windowNode
                }
                if (it) {
                    inspectorView.showLayoutBounds()
                } else {
                    inspectorView.hideLayoutBounds()
                }
            }
            inspector.observe(vm.isCollapsed) {
                rootView.isVisible = !it
                if (it) {
                    if (vm.currentMode eq InspectorMode.UI_OBJECT) {
                        screenshot?.recycle()
                        screenshot = null
                        windowNode = null
                        vm.highlightNode.value = null
                        inspectorView.clearNode()
                        inspectorView.background = null
                    } else if (vm.currentMode eq InspectorMode.GESTURE_RECORDER) {
                        gestureRecorder.deactivate()
                    }
                } else {
                    if (vm.currentMode eq InspectorMode.GESTURE_RECORDER) {
                        vm.makeToast(R.string.tip_gesture_recorder)
                        gestureRecorder.activate()
                    } else if (vm.currentMode eq InspectorMode.UI_OBJECT) captureWindowSnapshot()
                }
            }
            inspector.observe(vm.highlightNode) {
                inspectorView.highlightNode = it
                inspectorView.invalidate()
            }
            inspector.observe(vm.currentMode) {
                inspectorView.isVisible =
                    it == InspectorMode.COORDS || it == InspectorMode.UI_OBJECT
                gestureRecorderView.isVisible = it == InspectorMode.GESTURE_RECORDER
            }
            inspector.observeTransient(vm.onCoordinateSelected) {
                if (binding.inspectorView.isPointerMoved()) {
                    context.launchAction(
                        FloatingInspector.ACTION_COORDINATE_SELECTED,
                        IntValueUtil.composeCoordinate(
                            binding.inspectorView.getCoordinateX(),
                            binding.inspectorView.getCoordinateY()
                        )
                    )
                } else {
                    toast(R.string.error_no_coordinate_selected)
                }
            }
            inspector.observeTransient(vm.onComponentSelected) {
                if (vm.currentComp.isNull()) {
                    toast(R.string.error_no_selection)
                } else {
                    context.launchRoute(FloatingInspector.ACTION_COMPONENT_SELECTED)
                }
            }
        }
    }

    private fun captureWindowSnapshot() {
        val windowRoot = a11yAutomatorService.rootInActiveWindow
        if (windowRoot == null) {
            toast(R.string.inspect_failed.text)
            vm.isCollapsed.value = true
            return
        }
        inspector.lifecycleScope.launch(Dispatchers.Default) {
            windowNode = windowRoot.freeze()
            vm.showGrids.notifySelfChanged(true)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        rootView.isVisible = false
        rootView.doOnPreDraw {
            Choreographer.getInstance().postFrameCallback {
                a11yAutomatorService.takeScreenshot(
                    Display.DEFAULT_DISPLAY, Dispatchers.Main.asExecutor(), screenshotCallback
                )
            }
        }
    }

    private val screenshotCallback by lazy {

        @RequiresApi(Build.VERSION_CODES.R)
        object : AccessibilityService.TakeScreenshotCallback {
            override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                try {
                    result.hardwareBuffer.use { buffer ->
                        screenshot = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                            ?.clip(binding.inspectorView.visibleBounds)
                    }
                } catch (t: Throwable) {
                    t.logcatStackTrace()
                    vm.makeToast(R.string.screenshot_failed.str)
                }
                vm.pinScreenShot.notifySelfChanged()
                rootView.isVisible = true
            }

            override fun onFailure(errorCode: Int) {
                vm.makeToast(R.string.format_screenshot_failed.format(errorCode))
                vm.pinScreenShot.notifySelfChanged()
                rootView.isVisible = true
            }
        }
    }

    override fun onGestureStarted(startDelay: Long) {

    }

    override fun onGestureEnded(gesture: PointerGesture, duration: Long) {
        binding.gestureRecorderView.clearDrawingPath()
        rootView.isVisible = false
        val start = SystemClock.uptimeMillis()
        rootView.postDelayed(DELAY_PERFORM_GESTURE.toLong()) {
            inspector.lifecycleScope.launch {
                vm.playbackGesture.value = gesture
                (a11yAutomatorService.uiAutomatorBridge.gestureController as A11yGestureController)
                    .performGesture(gesture) { curDuration, succeeded ->
                        if (succeeded != null) {
                            rootView.isVisible = true
                            vm.onGesturePerformed.value = true
                        } else {
                            vm.currentDuration.value = curDuration
                        }
                        gestureRecorder.extraDelay = SystemClock.uptimeMillis() - start
                    }
            }
        }
    }

    override fun onGestureCancelled() {
        binding.gestureRecorderView.clearDrawingPath()
    }
}