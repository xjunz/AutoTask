/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.button.MaterialButton
import top.xjunz.tasker.R

/**
 * @author xjunz 2022/12/24
 */
class MaterialButtonSpreadContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var spreadHorizontal: Int = 0

    private var spreadVertical: Int = 0

    private var spreadCount = 5

    private var spreadDuration = 3_200

    private var spreadAlpha = 1F

    private var spreadCenterColor = -1

    private val centerPaint by lazy {
        Paint(paint).apply {
            color = spreadCenterColor
            style = Paint.Style.FILL
        }
    }

    init {
        setWillNotDraw(false)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MaterialButtonSpreadContainer)
        spreadHorizontal =
            ta.getDimensionPixelSizeOrThrow(R.styleable.MaterialButtonSpreadContainer_spreadHorizontal)
        spreadVertical =
            ta.getDimensionPixelSizeOrThrow(R.styleable.MaterialButtonSpreadContainer_spreadVertical)
        paint.color = ta.getColorOrThrow(R.styleable.MaterialButtonSpreadContainer_spreadColor)
        spreadAlpha =
            ta.getFloat(R.styleable.MaterialButtonSpreadContainer_spreadAlpha, spreadAlpha)
        spreadCenterColor = ta.getColor(
            R.styleable.MaterialButtonSpreadContainer_spreadCenterColor,
            spreadCenterColor
        )
        val spreadStyle = ta.getInt(R.styleable.MaterialButtonSpreadContainer_spreadStyle, 0)
        if (spreadStyle == 0) {
            paint.style = Paint.Style.STROKE
        } else if (spreadStyle == 1) {
            paint.style = Paint.Style.FILL
        }
        spreadCount = ta.getInt(R.styleable.MaterialButtonSpreadContainer_spreadCount, spreadCount)
        spreadDuration =
            ta.getInt(R.styleable.MaterialButtonSpreadContainer_spreadDuration, spreadDuration)
        if (ta.getBoolean(R.styleable.MaterialButtonSpreadContainer_spreadAutoStart, true)) {
            doOnPreDraw {
                startSpreading()
            }
        }
        ta.recycle()
    }

    private val button: MaterialButton by lazy {
        check(childCount == 1)
        getChildAt(0) as MaterialButton
    }

    private val spreadFractions by lazy {
        FloatArray(spreadCount)
    }

    private val animator = ValueAnimator.ofFloat(0F, 1F).apply {
        var prev = 0F
        addUpdateListener {
            val f = it.animatedFraction
            val delta = if (f < prev) 1 - prev else f - prev
            for (i in spreadFractions.indices) {
                spreadFractions[i] = (spreadFractions[i] + delta) % 1
            }
            prev = f
            invalidate()
        }
        interpolator = FastOutSlowInInterpolator()
        duration = spreadDuration.toLong()
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
    }

    private val drawingRect = RectF()

    private fun lerp(start: Number, stop: Number, f: Float): Float {
        return start.toFloat() + f * (stop.toFloat() - start.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val maxRadius = spreadVertical + height / 2
        val minRadius = (button.height - button.insetBottom - button.insetTop) / 2

        val horizontalRadius = spreadHorizontal + width / 2 - button.width / 2
        val verticalRadius = maxRadius - minRadius

        for (i in 0..spreadCount) {
            val f = if (i == spreadCount) 0F else spreadFractions[i]
            if (f < 0) continue

            drawingRect.left = lerp(button.left, button.left - horizontalRadius, f)
            drawingRect.right = lerp(button.right, button.right + horizontalRadius, f)

            val buttonTop = button.top + button.insetTop.toFloat()
            drawingRect.top = lerp(buttonTop, buttonTop - verticalRadius, f)
            val buttonBottom = button.bottom - button.insetBottom.toFloat()
            drawingRect.bottom = lerp(buttonBottom, buttonBottom + verticalRadius, f)

            paint.alpha = lerp(spreadAlpha * 0xFF, 0, f).toInt()
            val r = lerp(minRadius, maxRadius, f)
            canvas.drawRoundRect(drawingRect, r, r, paint)

            if (spreadCenterColor == -1) continue
            drawingRect.set(button.left.toFloat(), buttonTop, button.right.toFloat(), buttonBottom)
            canvas.drawRoundRect(drawingRect, minRadius.toFloat(), minRadius.toFloat(), centerPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearSpreading()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as ViewGroup).clipChildren = false
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        button.updateLayoutParams<LayoutParams> {
            gravity = Gravity.CENTER
        }
    }

    fun clearSpreading() {
        if (animator.isStarted) {
            animator.end()
            for (i in spreadFractions.indices) {
                spreadFractions[i] = 0F
            }
            invalidate()
        }
    }

    fun startSpreading() {
        if (!animator.isStarted) {
            for (i in spreadFractions.indices) {
                spreadFractions[i] = -i.toFloat() / spreadCount
            }
            animator.start()
        }
    }
}