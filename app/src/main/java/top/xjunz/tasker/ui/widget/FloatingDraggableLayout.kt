/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.hypot

/**
 * @author xjunz 2021/9/21
 */
class FloatingDraggableLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    companion object {
        const val STATE_DRAG_STARTED = 0
        const val STATE_DRAGGING = 1
        const val STATE_DRAG_ENDED = 2
    }

    private val downPoint = FloatArray(2)

    private val currentPoint = FloatArray(2)

    private var capturedView: View? = null

    private var isDragging = false

    var onDragListener: ((state: Int, offsetX: Float, offsetY: Float) -> Unit)? = null

    private fun findTopChildUnder(x: Float, y: Float): View? {
        val childCount: Int = childCount
        for (i in childCount - 1 downTo 0) {
            val child: View = getChildAt(i)
            if (x >= child.left && x < child.right && y >= child.top && y < child.bottom) {
                return child
            }
        }
        return null
    }

    /**
     * Intercept the touch event when the child underneath is not going to handle the event, e.g., its
     * [View.onTouchEvent] returns false, or when the [MotionEvent] has moved an as enough long distance
     * as the [touchSlop]. When the touch event is intercepted, we will call drag events via [onDragListener].
     *
     * @see onTouchEvent
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val rawX = ev.rawX
        val rawY = ev.rawY
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downPoint[0] = rawX
                downPoint[1] = rawY
                currentPoint[0] = rawX
                currentPoint[1] = rawY
                capturedView = findTopChildUnder(ev.x, ev.y)
                return capturedView == null
            }
            MotionEvent.ACTION_MOVE -> {
                val view = findTopChildUnder(ev.x, ev.y)
                if (capturedView != null && capturedView == view) {
                    if (hypot(rawX - downPoint[0], rawY - downPoint[1]) < touchSlop) {
                        // not a enough long distance travelled to confirm a drag
                        return false
                    }
                }
                // start drag myself
                isDragging = true
                onDragListener?.invoke(STATE_DRAG_STARTED, 0F, 0F)
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                onDragListener?.invoke(
                    STATE_DRAGGING, rawX - currentPoint[0], rawY - currentPoint[1]
                )
                currentPoint[0] = rawX
                currentPoint[1] = rawY
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    onDragListener?.invoke(STATE_DRAG_ENDED, 0F, 0F)
                }
                isDragging = false
                capturedView = null
            }
        }
        return true
    }
}
