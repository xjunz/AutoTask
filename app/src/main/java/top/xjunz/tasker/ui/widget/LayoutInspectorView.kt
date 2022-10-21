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
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.core.os.HandlerCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.dpFloat
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
    var emphaticNode: StableNodeInfo? = null

    private val boundsPaint = Paint().apply {
        style = Style.STROKE
        strokeWidth = 1.dpFloat
        color = Color.RED
    }

    private val emphaticPaint = Paint(boundsPaint).apply {
        color = Color.YELLOW
        strokeWidth = 2.dpFloat
    }

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Style.FILL
        textSize = 12.dpFloat
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

    private fun requireRootNode() = requireNotNull(rootNode) { "The root node is not set!" }

    private fun requireEmphaticNode() =
        requireNotNull(emphaticNode) { "There is no emphatic node!" }

    fun clearNode() {
        rootNode = null
        emphaticNode = null
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
        emphaticNode = null
    }

    fun hasNodeInfo(): Boolean {
        return rootNode != null
    }

    private fun offsetPointer(xOffset: Int, yOffset: Int) {
        pointerX = MathUtils.clamp(pointerX + xOffset, 0F, width.toFloat())
        pointerY = MathUtils.clamp(pointerY + yOffset, 0F, height.toFloat())
        emphaticNode = findNodeUnder(pointerX.toInt(), pointerY.toInt())
        invalidate()
    }

    private var offsetX: Int = 0

    private var offsetY: Int = 0

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

    private fun drawEmphaticBounds(canvas: Canvas, node: StableNodeInfo) {
        if (!shouldDrawNodes) {
            emphaticPaint.color = Color.RED
        } else {
            emphaticPaint.color = Color.YELLOW
        }
        reusableRect.set(node.getVisibleBoundsIn(visibleBounds))
        reusableRect.inset(
            (boundsPaint.strokeWidth / 2).toInt(),
            (boundsPaint.strokeWidth / 2).toInt()
        )
        canvas.drawRect(reusableRect, emphaticPaint)
    }

    private fun drawNormalNode(canvas: Canvas, node: StableNodeInfo) {
        node.children.forEach {
            reusableRect.set(it.getVisibleBoundsIn(visibleBounds))
            if (reusableRect.width() != 0 && reusableRect.height() != 0) {
                reusableRect.inset(
                    (boundsPaint.strokeWidth / 2).toInt(),
                    (boundsPaint.strokeWidth / 2).toInt()
                )
                canvas.drawRect(reusableRect, boundsPaint)
                drawNormalNode(canvas, it)
            }
        }
    }

    private val pointerDrawable =
        ContextCompat.getDrawable(context, R.drawable.ic_twotone_navigation_24)!!

    private val reusableRect = Rect()

    private fun drawTextAndBackground(canvas: Canvas, text: String, gravity: Int) {
        val fontMetrics = textPaint.fontMetrics
        textPaint.getTextBounds(text, 0, text.length, reusableRect)
        val left = if (gravity == Gravity.END) width - reusableRect.width() else 0
        reusableRect.offset(left, -fontMetrics.ascent.toInt())
        canvas.drawRect(reusableRect, textBgPaint)
        canvas.drawText(text, left.toFloat(), -fontMetrics.ascent, textPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rootNode != null && shouldDrawNodes) {
            canvas.drawRect(requireRootNode().getVisibleBoundsIn(visibleBounds), boundsPaint)
            drawNormalNode(canvas, requireRootNode())
        }
        if (emphaticNode != null) {
            drawEmphaticBounds(canvas, requireEmphaticNode())
        }
        // Draw coordinates
        drawTextAndBackground(
            canvas, "x:${pointerX.toInt() + offsetX}, y:${pointerY.toInt() + offsetY}",
            Gravity.END
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
        emphaticNode?.let {
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
            if (node != emphaticNode && node != null) {
                onNodeSelectedListener?.invoke(node)
            }
            emphaticNode = node
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
