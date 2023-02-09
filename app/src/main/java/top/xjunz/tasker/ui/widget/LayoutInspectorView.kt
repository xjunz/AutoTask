/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Rect
import android.os.Looper
import android.text.TextPaint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.core.os.HandlerCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.ktx.getDrawable
import top.xjunz.tasker.ktx.useStyledAttributes
import top.xjunz.tasker.task.inspector.StableNodeInfo
import kotlin.math.hypot


/**
 * @author xjunz 2021/9/17
 */
class LayoutInspectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var rootNode: StableNodeInfo? = null

    /**
     * The node which is currently being touched or being focused.
     */
    var highlightNode: StableNodeInfo? = null

    private val normalPaint = Paint().apply {
        style = Style.STROKE
    }

    private val highlightPaint = Paint(normalPaint)

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Style.FILL
    }

    private val textBgPaint = Paint().apply {
        color = ColorUtils.setAlphaComponent(Color.BLACK, (0.5F * 0XFF).toInt())
    }

    private var actionDownX = 0F

    private var actionDownY = 0F

    private var isOverSlop = false

    val visibleBounds = Rect()

    private val pointerRadius = 10.dpFloat
    private var pointerX = -1F
    private var pointerY = -1F

    private var shouldDrawNodes: Boolean = true

    /**
     * The listener to be notified when a node is selected by the user.
     */
    var onNodeClickedListener: ((StableNodeInfo) -> Unit)? = null

    var onNodeSelectedListener: ((StableNodeInfo) -> Unit)? = null

    init {
        useStyledAttributes(attrs, R.styleable.LayoutInspectorView) {
            normalPaint.color =
                it.getColorOrThrow(R.styleable.LayoutInspectorView_inspectorStrokeColorNormal)
            highlightPaint.color =
                it.getColorOrThrow(R.styleable.LayoutInspectorView_inspectorStrokeColorHighlight)
            normalPaint.strokeWidth =
                it.getDimensionOrThrow(R.styleable.LayoutInspectorView_inspectorStrokeWidthNormal)
            highlightPaint.strokeWidth =
                it.getDimensionOrThrow(R.styleable.LayoutInspectorView_inspectorStrokeWidthHighlight)
            textPaint.textSize =
                it.getDimensionOrThrow(R.styleable.LayoutInspectorView_inspectorCoordinateTextSize)
        }
    }

    private fun requireRootNode() = requireNotNull(rootNode) {
        "The root node is not set!"
    }

    private fun requireEmphaticNode() = requireNotNull(highlightNode) {
        "There is no emphatic node!"
    }

    fun clearNode() {
        rootNode = null
        highlightNode = null
        invalidate()
    }

    fun hideLayoutBounds() {
        shouldDrawNodes = false
        invalidate()
    }

    fun showLayoutBounds() {
        shouldDrawNodes = true
        invalidate()
    }

    fun setRootNode(node: StableNodeInfo) {
        rootNode = node
        highlightNode = null
    }

    fun hasNodeInfo(): Boolean {
        return rootNode != null
    }

    private fun offsetPointer(xOffset: Int, yOffset: Int) {
        pointerX = MathUtils.clamp(pointerX + xOffset, 0F, width.toFloat())
        pointerY = MathUtils.clamp(pointerY + yOffset, 0F, height.toFloat())
        highlightNode = findNodeUnder(pointerX.toInt(), pointerY.toInt())
        invalidate()
    }

    private var offsetX: Int = 0

    private var offsetY: Int = 0

    fun isPointerMoved(): Boolean {
        return pointerX >= 0 && pointerY >= 0
    }

    fun getCoordinateX(): Int {
        return (pointerX + offsetX).toInt()
    }

    fun getCoordinateY(): Int {
        return (pointerY + offsetY).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        getWindowVisibleDisplayFrame(visibleBounds)
        offsetX = visibleBounds.left
        offsetY = visibleBounds.top
    }

    private fun findNodeUnder(x: Int, y: Int): StableNodeInfo? {
        if (x <= 0 || y <= 0 || x >= width || y >= height) {
            return rootNode
        }
        return rootNode?.findNodeUnder(x, y, visibleBounds)
    }

    private fun makePointerMove(keyCode: Int, offset: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> offsetPointer(0, -offset)
            KeyEvent.KEYCODE_DPAD_DOWN -> offsetPointer(0, offset)
            KeyEvent.KEYCODE_DPAD_LEFT -> offsetPointer(-offset, 0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> offsetPointer(offset, 0)
        }
    }

    private var isKeyLongPressing = false

    private val pointerMoveHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (isKeyLongPressing) {
                pointerMoveHandler.removeCallbacksAndMessages(null)
            } else {
                makePointerMove(keyCode, 2)
            }
            isKeyLongPressing = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            isKeyLongPressing = true
            pointerMoveHandler.post(object : Runnable {
                override fun run() {
                    makePointerMove(keyCode, 3)
                    pointerMoveHandler.postDelayed(this, 16)
                }
            })
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    private fun drawHighlightBounds(canvas: Canvas, node: StableNodeInfo) {
        reusableRect.set(node.getVisibleBoundsIn(visibleBounds))
        reusableRect.inset(
            (normalPaint.strokeWidth / 2).toInt(),
            (normalPaint.strokeWidth / 2).toInt()
        )
        canvas.drawRect(reusableRect, highlightPaint)
    }

    private fun drawNormalNode(canvas: Canvas, node: StableNodeInfo) {
        node.children.forEach {
            reusableRect.set(it.getVisibleBoundsIn(visibleBounds))
            if (reusableRect.width() != 0 && reusableRect.height() != 0) {
                reusableRect.inset(
                    (normalPaint.strokeWidth / 2).toInt(),
                    (normalPaint.strokeWidth / 2).toInt()
                )
                canvas.drawRect(reusableRect, normalPaint)
                drawNormalNode(canvas, it)
            }
        }
    }

    private val pointerDrawable = R.drawable.ic_twotone_navigation_24.getDrawable()

    private val reusableRect = Rect()

    private fun drawTextAndBackground(canvas: Canvas, text: String) {
        val fontMetrics = textPaint.fontMetrics
        textPaint.getTextBounds(text, 0, text.length, reusableRect)
        val left = width - reusableRect.width()
        reusableRect.offset(left, -fontMetrics.ascent.toInt())
        canvas.drawRect(reusableRect, textBgPaint)
        canvas.drawText(text, left.toFloat(), -fontMetrics.ascent, textPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rootNode != null && shouldDrawNodes) {
            canvas.drawRect(requireRootNode().getVisibleBoundsIn(visibleBounds), normalPaint)
            drawNormalNode(canvas, requireRootNode())
        }
        if (highlightNode != null) {
            drawHighlightBounds(canvas, requireEmphaticNode())
        }
        // Draw coordinates
        drawTextAndBackground(
            canvas, "x:${getCoordinateX()}, y:${getCoordinateY()}"
        )
        if (pointerX >= 0) {
            reusableRect.set(
                (pointerX - pointerRadius).toInt(), pointerY.toInt(),
                (pointerX + pointerRadius).toInt(), (pointerY + pointerRadius * 2).toInt()
            )
            pointerDrawable.bounds = reusableRect
            pointerDrawable.draw(canvas)
        }
    }

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun performClick(): Boolean {
        highlightNode?.let {
            onNodeClickedListener?.invoke(it)
        }
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        pointerX = x
        pointerY = y
        if (rootNode != null) {
            val node = findNodeUnder(x.toInt(), y.toInt())
            if (node != highlightNode && node != null) {
                onNodeSelectedListener?.invoke(node)
            }
            highlightNode = node
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                actionDownX = x
                actionDownY = y
            }
            MotionEvent.ACTION_MOVE -> {
                isOverSlop = hypot(x - actionDownX, y - actionDownY) > touchSlop
            }
            MotionEvent.ACTION_UP -> {
                if (!isOverSlop) performClick()
                isOverSlop = false
            }
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pointerMoveHandler.removeCallbacksAndMessages(null)
    }

}
