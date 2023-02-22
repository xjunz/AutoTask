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
import android.view.Display
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.test.uiautomator.PointerGesture
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayInspectorBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.gesture.GestureRecorder
import top.xjunz.tasker.task.gesture.SerializableInputEvent
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.task.inspector.StableNodeInfo
import top.xjunz.tasker.task.inspector.StableNodeInfo.Companion.freeze
import top.xjunz.tasker.ui.main.EventCenter

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
                if (it) {
                    rootView.isVisible = false
                    if (vm.currentMode eq InspectorMode.UI_OBJECT) {
                        screenshot?.recycle()
                        screenshot = null
                        windowNode = null
                        vm.highlightNode.value = null
                        inspectorView.clearNode()
                        inspectorView.background = null
                    } else if (vm.currentMode eq InspectorMode.GESTURE_RECORDER) {
                        gestureRecorder.deactivate()
                        vm.isRecordingGesture.value = false
                    }
                } else {
                    rootView.isVisible = true
                    if (vm.currentMode eq InspectorMode.GESTURE_RECORDER) {
                        if (vm.recordedEvents.require().isEmpty()) {
                            vm.makeToast(R.string.tip_gesture_recorder)
                            gestureRecorder.activate()
                            vm.clearAllRecordedEvents()
                            vm.isRecordingGesture.value = true
                        } else {
                            rootView.isVisible = false
                        }
                    } else if (vm.currentMode eq InspectorMode.TASK_ASSISTANT) {
                        rootView.isVisible = false
                    } else if (vm.currentMode eq InspectorMode.UI_OBJECT) {
                        captureWindowSnapshot()
                    }
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
                    EventCenter.routeEvent(
                        FloatingInspector.EVENT_COORDINATE_SELECTED,
                        IntValueUtil.composeXY(
                            binding.inspectorView.getCoordinateX(),
                            binding.inspectorView.getCoordinateY()
                        )
                    )
                } else {
                    toast(R.string.error_no_coordinate_selected)
                }
            }
            inspector.observeTransient(vm.onComponentSelected) {
                if (vm.currentComponent.isNull()) {
                    toast(R.string.error_no_selection)
                } else {
                    EventCenter.routeEvent(
                        FloatingInspector.EVENT_COMPONENT_SELECTED,
                        vm.currentComponent.require()
                    )
                }
            }
            inspector.observeTransient(vm.requestRecordingState) {
                rootView.isVisible = it
                if (it) {
                    gestureRecorder.activate()
                } else {
                    gestureRecorder.deactivate()
                    vm.showGestures.value = true
                }
                vm.isRecordingGesture.value = it
            }
            inspector.observeTransient(vm.requestReplayGestures) {
                inspector.lifecycleScope.launch {
                    delay(300)
                    for (event in it) {
                        if (event.type == SerializableInputEvent.INPUT_TYPE_KEY) {
                            event.execute()
                        } else a11yAutomatorService.gestureController.performSinglePointerGestures(
                            event.getGesture()
                        ) { curDuration, finished ->
                            if (finished) {
                                vm.onGesturePlaybackEnded.value = true
                            } else if (curDuration == 0L) {
                                vm.onGesturePlaybackStarted.value = event.getGesture()
                            } else {
                                vm.currentGesturePlaybackDuration.value = curDuration
                            }
                        }
                    }
                    a11yAutomatorService.a11yEventDispatcher.waitForIdle(500, 5000)
                    if (vm.showGestures.isNotTrue) {
                        vm.makeToast(R.string.playback_finished)
                        vm.showGestures.value = true
                    }
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
            inspector.lifecycleScope.launch {
                awaitFrame()
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

    override fun onLongClickDetected() {
        binding.gestureRecorderView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun onGestureStarted(startDelay: Long) {
    }

    override fun onGestureEnded(gesture: PointerGesture, duration: Long) {
        binding.gestureRecorderView.clearDrawingPath()
        rootView.isVisible = false
        val start = SystemClock.uptimeMillis()
        inspector.lifecycleScope.launch {
            delay(DELAY_PERFORM_GESTURE.toLong())
            vm.onGesturePlaybackStarted.value = gesture
            a11yAutomatorService.gestureController.performSinglePointerGesture(
                true, gesture
            ) { curDuration, succeeded ->
                if (succeeded != null) {
                    rootView.isVisible = true
                    vm.onGesturePlaybackEnded.value = true
                    if (succeeded) {
                        vm.recordGesture(gesture)
                    }
                } else {
                    vm.currentGesturePlaybackDuration.value = curDuration
                }
                gestureRecorder.extraDelay = SystemClock.uptimeMillis() - start
            }
        }
    }

    override fun onGestureCancelled() {
        binding.gestureRecorderView.clearDrawingPath()
    }
}