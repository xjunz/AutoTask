/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.transition.Transition
import android.transition.TransitionManager
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowInsets.Side
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.core.transition.doOnEnd
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.transition.platform.MaterialSharedAxis
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.OverlayToastBinding
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.isPrivilegedProcess
import top.xjunz.tasker.ktx.italic
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.ui.widget.DrawBoundsFrameLayout
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author xjunz 2023/02/01
 */
class OverlayToastBridge(looper: Looper) {

    companion object {
        private const val QUEUE_MAX_MESSAGE_COUNT = 40
        private const val TOAST_DURATION_MILLS = 1500
        private const val CLEAR_BOUNDS_DELAY = 5_000L
    }

    private val handler: Handler by lazy {
        Handler(looper)
    }

    private val context by lazy {
        if (isAppProcess) {
            ContextThemeWrapper(A11yAutomatorService.require(), R.style.AppTheme)
        } else {
            ThemedWindowContext(ContextBridge.getAppResourceContext(), Binder())
        }
    }

    private val windowManager by lazy {
        context.getSystemService(WindowManager::class.java)
    }

    private lateinit var textView: TextView

    private val container: DrawBoundsFrameLayout by lazy {
        if (isPrivilegedProcess) {
            val field = context.baseContext.javaClass.getDeclaredField("mClassLoader")
            field.isAccessible = true
            field.set(context.baseContext, javaClass.classLoader)
        }
        val binding = OverlayToastBinding.inflate(LayoutInflater.from(context))
        textView = binding.tvToast
        binding.root.casted()
    }

    private val baseLayoutParams
        get() = WindowManager.LayoutParams().apply {
            type = if (isAppProcess) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else (WindowManager.LayoutParams::class.java.getField("TYPE_SECURE_SYSTEM_OVERLAY")[null] as Int).toInt()
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsSides = Side.all()
                fitInsetsTypes = WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            }
        }

    private val queue: Queue<CharSequence?> = ConcurrentLinkedQueue<CharSequence?>()

    private fun transitToastToShow(msg: CharSequence?, onEnd: (Transition) -> Unit) {
        val transition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            .setInterpolator(FastOutSlowInInterpolator())
        transition.doOnEnd(onEnd)
        if (!::textView.isInitialized) {
            windowManager.addView(container, baseLayoutParams)
            container.doOnPreDraw {
                TransitionManager.beginDelayedTransition(container, transition)
                textView.isVisible = true
            }
        } else {
            TransitionManager.beginDelayedTransition(container, transition)
            textView.isVisible = true
        }
        textView.text = msg ?: "null".italic()
    }

    private inline fun transitToastToDismiss(crossinline onEnd: () -> Unit) {
        if (textView.isVisible) {
            val transition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
            transition.doOnEnd {
                onEnd()
            }
            TransitionManager.beginDelayedTransition(container, transition)
            textView.isVisible = false
        } else {
            onEnd()
        }
    }

    private fun showQueued() {
        handler.post {
            transitToastToShow(queue.peek()) {
                handler.postDelayed(TOAST_DURATION_MILLS - it.duration) {
                    transitToastToDismiss {
                        queue.poll()
                        if (queue.isNotEmpty()) showQueued()
                    }
                }
            }
        }
    }

    private fun enqueueMessage(msg: CharSequence?) {
        if (queue.size < QUEUE_MAX_MESSAGE_COUNT) {
            queue.offer(msg)
        }
    }

    @Synchronized
    fun showOverlayToast(msg: CharSequence?) {
        if (queue.isEmpty()) {
            enqueueMessage(msg)
            showQueued()
        } else {
            enqueueMessage(msg)
        }
    }

    private val clearBoundsTask = Runnable {
        container.clearBounds()
    }

    fun drawAccessibilityBounds(node: AccessibilityNodeInfo) {
        if (!::textView.isInitialized) {
            handler.post {
                windowManager.addView(container, baseLayoutParams)
                postDrawAccessibilityBounds(node)
            }
        } else {
            postDrawAccessibilityBounds(node)
        }
    }

    private fun postDrawAccessibilityBounds(node: AccessibilityNodeInfo) {
        handler.removeCallbacks(clearBoundsTask)
        container.drawAccessibilityNode(node)
        handler.postDelayed(clearBoundsTask, CLEAR_BOUNDS_DELAY)
    }

    fun clearAccessibilityBounds() {
        clearBoundsTask.run()
    }

    fun destroy() {
        if (container.isAttachedToWindow) {
            windowManager.removeView(container)
            queue.clear()
            handler.removeCallbacksAndMessages(null)
        }
    }
}