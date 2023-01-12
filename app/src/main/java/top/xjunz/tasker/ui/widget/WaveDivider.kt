/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.view.doOnPreDraw
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import top.xjunz.tasker.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * @author xjunz 2023/01/11
 */
class WaveDivider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var waveStrokeColor = 0
    private var waveRunningColor = 0

    private val argbEvaluator = ArgbEvaluator()

    private val offsetAnimator by lazy {
        ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener {
                val f = it.animatedFraction
                waveOffset = -(width / waveWidth) * waveWidth * f
                invalidate()
            }
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            duration = 10 * 1000
        }
    }


    private val fadeAnimator by lazy {
        ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener {
                val f = it.animatedFraction
                if (shouldFadeOut) {
                    paint.color =
                        argbEvaluator.evaluate(f, previousPaintColor, waveStrokeColor) as Int
                    waveMaxHeight = (1 - f) * previousWaveHeight
                } else if (shouldFadeIn) {
                    paint.color =
                        argbEvaluator.evaluate(f, previousPaintColor, waveRunningColor) as Int
                    waveMaxHeight = (height / 2F - previousWaveHeight) * f + previousWaveHeight
                } else {
                    error("Not fading out or fading in!")
                }
                invalidate()
            }
            interpolator = FastOutSlowInInterpolator()
            duration = 800
        }
    }

    private var previousPaintColor = 0

    private var previousWaveHeight = 0F

    private var shouldFadeOut = false

    private var shouldFadeIn = false

    private fun performFadeOut() {
        if (waveMaxHeight == 0F) return
        if (shouldFadeOut) return
        shouldFadeOut = true
        fadeAnimator.doOnEnd {
            offsetAnimator.cancel()
            shouldFadeOut = false
        }
        fadeAnimator.doOnCancel {
            shouldFadeOut = false
        }
        startFading()
    }

    fun fadeOut() {
        if (isLaidOut) {
            performFadeOut()
        } else {
            doOnPreDraw {
                performFadeOut()
            }
        }
    }

    fun pause() {
        if (fadeAnimator.isStarted)
            fadeAnimator.pause()
        if (offsetAnimator.isStarted)
            offsetAnimator.pause()
    }

    fun resume() {
        if (fadeAnimator.isPaused)
            fadeAnimator.resume()
        if (offsetAnimator.isPaused)
            offsetAnimator.resume()
    }

    private fun startFading() {
        if (fadeAnimator.isStarted) {
            fadeAnimator.cancel()
        }
        previousPaintColor = paint.color
        previousWaveHeight = waveMaxHeight
        fadeAnimator.start()
    }

    private fun performFadeIn() {
        if (waveMaxHeight == height / 2F) return
        if (shouldFadeIn) return
        shouldFadeIn = true
        fadeAnimator.doOnEnd {
            offsetAnimator.start()
            shouldFadeIn = false
        }
        fadeAnimator.doOnCancel {
            shouldFadeIn = false
        }
        startFading()
    }

    fun fadeIn() {
        if (isLaidOut) {
            performFadeIn()
        } else {
            doOnPreDraw {
                performFadeIn()
            }
        }
    }

    private var waveMaxHeight = 0F

    private var waveWidth = 0

    private var waveOffset = 0F

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val path = Path()

    private fun buildPath() {
        path.reset()
        val h = (waveMaxHeight - paint.strokeWidth).coerceAtLeast(0F)
        if (h == 0F) {
            path.moveTo(0F, height / 2F)
            path.lineTo(width.toFloat(), height / 2F)
        } else {
            for (i in 0..width) {
                val x = i.toFloat()
                val y = h * sin(
                    ((i + waveOffset) % waveWidth) / waveWidth * (2 * PI)
                ).toFloat() + height / 2
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
        }
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.WaveDivider).use {
            paint.strokeWidth =
                it.getDimensionPixelSizeOrThrow(R.styleable.WaveDivider_waveStrokeWidth).toFloat()
            waveStrokeColor = it.getColorOrThrow(R.styleable.WaveDivider_waveStrokeColor)
            paint.color = waveStrokeColor
            waveRunningColor = it.getColorOrThrow(R.styleable.WaveDivider_waveRunningColor)
            waveWidth = it.getDimensionPixelSizeOrThrow(R.styleable.WaveDivider_waveWidth)
        }
    }

    private class SavedState : BaseSavedState {

        var currentWaveHeight: Float = 0F
        var currentOffset: Float = 0F
        var currentPaintColor: Int = 0
        var shouldFadeIn = false
        var shouldFadeOut = false

        constructor(parcel: Parcel) : super(parcel) {
            currentWaveHeight = parcel.readFloat()
            currentOffset = parcel.readFloat()
            currentPaintColor = parcel.readInt()
            shouldFadeIn = parcel.readByte() != 0.toByte()
            shouldFadeOut = parcel.readByte() != 0.toByte()
        }

        constructor(parcelable: Parcelable?) : super(parcelable)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeFloat(currentWaveHeight)
            parcel.writeFloat(currentOffset)
            parcel.writeInt(currentPaintColor)
            parcel.writeByte(if (shouldFadeIn) 1 else 0)
            parcel.writeByte(if (shouldFadeOut) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (state != null) {
            state as SavedState
            waveMaxHeight = state.currentWaveHeight
            waveOffset = state.currentOffset
            paint.color = state.currentPaintColor
            if (state.shouldFadeIn) {
                fadeIn()
            } else if (state.shouldFadeOut) {
                fadeOut()
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = SavedState(super.onSaveInstanceState())
        state.currentOffset = waveOffset
        state.shouldFadeIn = shouldFadeIn
        state.shouldFadeOut = shouldFadeOut
        state.currentPaintColor = paint.color
        state.currentWaveHeight = waveMaxHeight
        return state
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (offsetAnimator.isPaused) {
            offsetAnimator.resume()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (offsetAnimator.isStarted) {
            offsetAnimator.pause()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        buildPath()
        canvas.drawPath(path, paint)
    }
}