package top.xjunz.tasker.task.inspector.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayInspectorBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.StableNodeInfo
import top.xjunz.tasker.task.inspector.StableNodeInfo.Companion.freeze

/**
 * @author xjunz 2022/10/16
 */
class InspectorViewOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayInspectorBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private var screenshot: Bitmap? = null

    private var windowNode: StableNodeInfo? = null

    override fun onOverlayInflated() {
        binding.apply {
            inspectorView.doOnPreDraw {
                vm.windowWidth = it.width
                vm.windowHeight = it.height
            }
            btnDismiss.setOnClickListener {
                nodePanel.isVisible = false
            }
            inspectorView.onNodeClickedListener = {
                vm.emphaticNode.setValueIfDistinct(it)
            }
            inspectorView.onNodeSelectedListener = {
                vm.emphaticNode.setValueIfDistinct(it)
            }
            inspector.observeTransient(vm.onKeyLongPressed) {
                inspectorView.onKeyLongPress(it, null)
            }
            inspector.observeTransient(vm.onKeyUpOrCancelled) {
                inspectorView.onKeyUp(it, null)
            }
            inspector.observe(vm.pinScreenShot) {
                // show rootView after screenshot
                rootView.isVisible = true
                if (it) {
                    if (screenshot != null) {
                        inspectorView.background = BitmapDrawable(context.resources, screenshot)
                    }
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
                    screenshot = null
                    windowNode = null
                    vm.emphaticNode.value = null
                    inspectorView.clearNode()
                    inspectorView.background = null
                } else {
                    rootView.isVisible = true
                    if (vm.currentMode eq FloatingInspector.MODE_UI_OBJECT) {
                        captureWindowSnapshot()
                    }
                }
            }
            inspector.observe(vm.emphaticNode) {
                inspectorView.emphaticNode = it
                inspectorView.invalidate()
            }
            inspector.observe(vm.currentMode) {
                inspectorView.isVisible = it != FloatingInspector.MODE_COMPONENT
            }
        }
    }


    private fun captureWindowSnapshot() {
        val windowRoot = A11yAutomatorService.require().rootInActiveWindow
        if (windowRoot == null) {
            toast(R.string.inspect_failed.text)
            vm.isCollapsed.value = true
            return
        }
        inspector.lifecycleScope.launch(Dispatchers.Default) {
            windowNode = windowRoot.freeze()
            vm.showGrids.notifySelfChanged(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootView.isVisible = false
            rootView.post {
                A11yAutomatorService.require().takeScreenshot(
                    Display.DEFAULT_DISPLAY, Dispatchers.IO.asExecutor(),
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                            try {
                                result.hardwareBuffer.use {
                                    val raw = Bitmap.wrapHardwareBuffer(
                                        it, result.colorSpace
                                    )
                                    screenshot = raw?.clip(binding.inspectorView.visibleBounds)
                                    vm.pinScreenShot.notifySelfChanged(true)
                                }
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                vm.toastText.postValue(R.string.screenshot_failed.str)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            vm.toastText.postValue(
                                R.string.format_screenshot_failed.format(errorCode)
                            )
                            vm.pinScreenShot.notifySelfChanged(true)
                        }
                    })
            }
        }
    }
}