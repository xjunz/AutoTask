/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import top.xjunz.tasker.ktx.alphaModified
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.task.gesture.GestureRecorder

/**
 * @author xjunz 2023/02/13
 */
class GestureRecorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val STROKE_WIDTH_IN_DP = 16
    }

    private lateinit var recorder: GestureRecorder

    private val drawingPath = Path()

    private val pathPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH_IN_DP.dpFloat
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.RED.alphaModified(.62F)
    }

    fun setRecorder(recorder: GestureRecorder) {
        this.recorder = recorder
    }

    fun clearDrawingPath() {
        drawingPath.reset()
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!recorder.isActivated) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drawingPath.reset()
                drawingPath.moveTo(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                drawingPath.lineTo(event.x, event.y)
                invalidate()
            }
        }
        return recorder.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(drawingPath, pathPaint)
    }
}