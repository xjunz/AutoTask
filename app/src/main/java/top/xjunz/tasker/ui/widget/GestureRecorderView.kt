/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.gesture.GestureOverlayView
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.animation.doOnEnd
import androidx.core.graphics.PathSegment
import androidx.core.graphics.flatten
import androidx.test.uiautomator.PointerGesture
import top.xjunz.tasker.service.a11yAutomatorService

/**
 * @author xjunz 2023/01/05
 */
class GestureRecorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : GestureOverlayView(context, attrs, defStyleAttr) {

    private val replayPath = Path()

    private val replayStrokes: MutableList<PathSegment> = mutableListOf()

    private var currentStroke: Int = -1

    private var duration: Long = 0

    private val animator = ValueAnimator.ofFloat(0F, 1F).apply {
        addUpdateListener {
            currentStroke = (it.animatedFraction * replayStrokes.lastIndex).toInt()
            invalidate()
        }
        doOnEnd {
            replayStrokes.clear()
            currentStroke = -1
        }
    }

    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            color = gestureColor
            strokeWidth = gestureStrokeWidth
        }
    }

    init {
        addOnGesturePerformedListener { _, _ ->
            replayStrokes.clear()
            val path = gesturePath
            replayStrokes.addAll(path.flatten())
            replayPath.reset()
            animator.duration = duration
            //animator.start()

            fun PointF.toPoint(): Point {
                return Point(x.toInt(), y.toInt())
            }

            fun PathSegment.getDuration(): Long {
                return (duration * (endFraction - startFraction)).toLong()
            }

            var gesture: PointerGesture? = null
            var start: PointF
            var stop: PointF

            for (segment in replayStrokes) {
                start = segment.start
                stop = segment.end
                if (gesture == null) {
                    gesture = PointerGesture(start.toPoint())
                        .moveWithDuration(stop.toPoint(), segment.getDuration())
                } else {
                    gesture.moveWithDuration(stop.toPoint(), segment.getDuration())
                }
            }
            a11yAutomatorService.uiAutomatorBridge.gestureController.performGesture(gesture)
        }
        addOnGesturingListener(object : OnGesturingListener {
            override fun onGesturingStarted(overlay: GestureOverlayView?) {
                duration = SystemClock.uptimeMillis()
            }

            override fun onGesturingEnded(overlay: GestureOverlayView?) {
                duration = SystemClock.uptimeMillis() - duration
            }

        })
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (animator.isRunning) {
            animator.cancel()
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (replayStrokes.isEmpty()) return
        replayPath.reset()
        var p = replayStrokes[0].start
        replayPath.moveTo(p.x, p.y)
        for (i in 0..currentStroke) {
            p = replayStrokes[i].end
            replayPath.lineTo(p.x, p.y)
        }
        canvas.drawPath(replayPath, paint)
    }

}