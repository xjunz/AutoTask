/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.getVisibleBoundsIn
import top.xjunz.tasker.ktx.useStyledAttributes

/**
 * @author xjunz 2023/03/14
 */
class DrawBoundsFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val boundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val visibleBounds = Rect()

    private var bounds: Rect? = null

    init {
        setWillNotDraw(false)
        useStyledAttributes(attrs, R.styleable.DrawBoundsFrameLayout) {
            boundsPaint.color =
                it.getColorOrThrow(R.styleable.DrawBoundsFrameLayout_boundsStrokeColor)
            boundsPaint.strokeWidth =
                it.getDimensionOrThrow(R.styleable.DrawBoundsFrameLayout_boundsStrokeWidth)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        getWindowVisibleDisplayFrame(visibleBounds)
    }

    fun drawAccessibilityNode(node: AccessibilityNodeInfo) {
        bounds = node.getVisibleBoundsIn(visibleBounds)
        postInvalidate()
    }

    fun clearBounds() {
        bounds = null
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localRect = bounds
        if (localRect != null && !localRect.isEmpty) {
            canvas.drawRect(localRect, boundsPaint)
        }
    }

}