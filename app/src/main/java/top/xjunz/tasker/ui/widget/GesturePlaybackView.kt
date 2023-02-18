/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.test.uiautomator.PointerGesture
import top.xjunz.tasker.ktx.alphaModified
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.ui.main.ColorScheme

/**
 * @author xjunz 2023/02/14
 */
class GesturePlaybackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val drawingPath = Path()

    private var gesture: PointerGesture? = null

    private val paint = Paint().apply {
        isAntiAlias = true
        color = ColorScheme.colorPrimary.alphaModified(.62F)
        strokeWidth = GestureRecorderView.STROKE_WIDTH_IN_DP.dpFloat
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
        drawingPath.moveTo(
            gesture.start().x.toFloat() - offsetX,
            gesture.start().y.toFloat() - offsetY
        )
        this.gesture = gesture
        if (alpha != 1F)
            animate().alpha(1F).start()
    }

    fun fadeOut() {
        clear()
        animate().alpha(0F).start()
    }

    fun clear() {
        drawingPath.reset()
        gesture = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(drawingPath, paint)
    }
}