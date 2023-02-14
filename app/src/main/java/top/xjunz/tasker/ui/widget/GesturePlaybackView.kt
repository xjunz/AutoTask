/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.test.uiautomator.PointerGesture
import top.xjunz.tasker.ktx.alphaModified
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.ui.ColorScheme

/**
 * @author xjunz 2023/02/14
 */
class GesturePlaybackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val drawingPath = Path()

    private var animator: ValueAnimator? = null

    private var gesture: PointerGesture? = null

    private val paint = Paint().apply {
        isAntiAlias = true
        color = ColorScheme.colorPrimary.alphaModified(.62F)
        strokeWidth = 8.dpFloat
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    fun updateCurrentDuration(duration: Long) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val offsetY = loc[1]
        val offsetX = loc[0]
        val point = gesture!!.pointAtIgnoringDelay(duration)
        drawingPath.lineTo(point.x.toFloat() - offsetX, point.y.toFloat() - offsetY)
        invalidate()
    }

    fun setGesture(gesture: PointerGesture) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val offsetY = loc[1]
        val offsetX = loc[0]
        animator = ValueAnimator.ofFloat(1F)
        drawingPath.moveTo(
            gesture.start().x.toFloat() - offsetX,
            gesture.start().y.toFloat() - offsetY
        )
        this.gesture = gesture
        if (alpha != 1F)
            animate().alpha(1F).start()
    }

    fun clear() {
        drawingPath.reset()
        animator?.cancel()
        gesture = null
        animate().alpha(0F).start()
        // invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(drawingPath, paint)
    }
}