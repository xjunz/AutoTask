/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.AccessibilityDelegate
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.math.MathUtils
import androidx.core.view.isVisible
import androidx.databinding.ViewDataBinding
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.ktx.doWhenEnd
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.util.Motions
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/10/16
 */
abstract class FloatingInspectorOverlay<B : ViewDataBinding>(val inspector: FloatingInspector) :
    AccessibilityDelegate() {

    protected val context get() = inspector.context

    protected val vm get() = inspector.viewModel
    protected fun updateViewLayout() {
        windowManager.updateViewLayout(rootView, layoutParams)
    }

    protected fun offsetViewInWindow(offsetX: Int, offsetY: Int) {
        check(layoutParams.gravity == Gravity.CENTER)
        layoutParams.x = MathUtils.clamp(
            layoutParams.x + offsetX,
            rootView.width / 2 - vm.windowWidth / 2,
            vm.windowWidth / 2 - rootView.width / 2
        )
        layoutParams.y = MathUtils.clamp(
            layoutParams.y + offsetY,
            rootView.height / 2 - vm.windowHeight / 2,
            vm.windowHeight / 2 - rootView.height / 2
        )
        updateViewLayout()
    }

    protected fun animateShow() {
        if (rootView.isVisible) return
        rootView.isVisible = true
        rootView.scaleX = .98F
        rootView.scaleY = .98F
        rootView.alpha = 0F
        rootView.animate().alpha(1F).scaleX(1F).setDuration(150)
            .setInterpolator(Motions.EASING_EMPHASIZED)
            .scaleY(1F)
    }

    protected fun animateHide() {
        if (!rootView.isVisible) return
        rootView.animate().alpha(0F).scaleX(.98F).setDuration(150)
            .setInterpolator(Motions.EASING_EMPHASIZED)
            .scaleY(.98F).doWhenEnd {
                rootView.isVisible = false
            }
    }

    protected val windowManager get() = inspector.windowManager

    private val layoutInflater = LayoutInflater.from(context)

    private val baseLayoutParams
        get() = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
        }

    private fun createOverlayView() {
        binding = javaClass.superClassFirstParameterizedType().getDeclaredMethod(
            "inflate", LayoutInflater::class.java
        ).invoke(null, layoutInflater)!!.casted()
    }

    protected lateinit var binding: B

    val rootView get() = binding.root

    val layoutParams: WindowManager.LayoutParams = baseLayoutParams

    override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(host, event)
        event.className = inspector.exemptionEventClassName
    }

    open fun onOverlayInflated() {
        rootView.accessibilityDelegate = this
    }

    open fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        /* no-op */
    }

    protected open fun onDismiss() {
        /* no-op */
    }

    fun init() {
        modifyLayoutParams(layoutParams)
        createOverlayView()
        onOverlayInflated()
    }

    fun removeFromWindowManager() {
        windowManager.removeView(rootView)
        onDismiss()
    }

    fun addToWindowManager() {
        windowManager.addView(rootView, layoutParams)
    }

}