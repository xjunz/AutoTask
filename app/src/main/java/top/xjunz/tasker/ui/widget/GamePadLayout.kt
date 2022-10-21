package top.xjunz.tasker.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import androidx.core.view.get
import kotlin.math.sqrt


/**
 * @author xjunz 2021/9/24
 */
class GamePadLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private object ChildIndex {
        const val REQUIRED_CHILD_COUNT = 5
        const val CENTER = 0
        const val LEFT = 1
        const val TOP = 2
        const val RIGHT = 3
        const val BOTTOM = 4
        val HORIZONTAL = arrayOf(LEFT, RIGHT)
        val VERTICAL = arrayOf(TOP, BOTTOM)
    }

    private var isCollapsed = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        check(childCount == ChildIndex.REQUIRED_CHILD_COUNT) { "having exactly 5 children is a must" }
        var desiredWidth = 0
        var desiredHeight = 0
        var childState = 0
        children.forEachIndexed { i, child ->
            if (child.visibility != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val lp = child.layoutParams as LayoutParams
                when (i) {
                    in ChildIndex.VERTICAL -> if (!isCollapsed) {
                        desiredHeight += child.measuredHeight + lp.topMargin + lp.bottomMargin
                    }
                    in ChildIndex.HORIZONTAL -> if (!isCollapsed) {
                        desiredWidth += child.measuredWidth + lp.leftMargin + lp.rightMargin
                    }
                    else -> {
                        desiredHeight += child.measuredHeight + lp.topMargin + lp.bottomMargin
                        desiredWidth += child.measuredWidth + lp.leftMargin + lp.rightMargin
                    }
                }
                childState = combineMeasuredStates(childState, child.measuredState)
            }
        }
        setMeasuredDimension(
            resolveSizeAndState(desiredWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                desiredHeight, heightMeasureSpec, childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val centerChild = getChildAt(ChildIndex.CENTER)
        val centerLp = centerChild.layoutParams as LayoutParams
        val centerLeft = measuredWidth / 2 - centerChild.measuredWidth / 2
        val centerTop = measuredHeight / 2 - centerChild.measuredHeight / 2
        children.forEachIndexed { index, child ->
            val lp = child.layoutParams as LayoutParams
            when (index) {
                ChildIndex.CENTER -> {
                    child.layout(
                        centerLeft, centerTop, centerLeft + centerChild.measuredWidth,
                        centerTop + centerChild.measuredHeight
                    )
                }
                ChildIndex.LEFT -> {
                    val right = centerLeft - lp.rightMargin - centerLp.leftMargin
                    child.layout(
                        right - child.measuredWidth, measuredHeight / 2 - child.measuredHeight / 2,
                        right, measuredHeight / 2 - child.measuredHeight / 2 + child.measuredHeight
                    )
                }
                ChildIndex.RIGHT -> {
                    val left =
                        centerLeft + centerChild.measuredWidth + lp.leftMargin + centerLp.rightMargin
                    child.layout(
                        left, measuredHeight / 2 - child.measuredHeight / 2,
                        left + child.measuredWidth,
                        measuredHeight / 2 - child.measuredHeight / 2 + child.measuredHeight
                    )
                }
                ChildIndex.TOP -> {
                    val bottom = centerTop - lp.bottomMargin - centerLp.topMargin
                    child.layout(
                        measuredWidth / 2 - child.measuredWidth / 2, bottom - child.measuredHeight,
                        measuredWidth / 2 - child.measuredWidth / 2 + child.measuredWidth, bottom
                    )
                }
                ChildIndex.BOTTOM -> {
                    val top =
                        centerTop + centerChild.measuredHeight + lp.topMargin + centerLp.bottomMargin
                    child.layout(
                        measuredWidth / 2 - child.measuredWidth / 2, top,
                        measuredWidth / 2 - child.measuredWidth / 2 + child.measuredWidth,
                        top + child.measuredHeight
                    )
                }
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    class LayoutParams : MarginLayoutParams {

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        constructor(width: Int, height: Int) : super(width, height)
    }

    private val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3F
        alpha = (.21F * 0xFF).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = this[ChildIndex.CENTER].width / 1.75F
        val centerX = width / 2F
        val centerY = height / 2F
        val sqrt2 = sqrt(2F)
        val minOffset = radius / sqrt2
        val maxOffset = width / 2 / sqrt2
        paint.strokeWidth = 4F
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.strokeWidth = 3F
        // horizontal
        canvas.drawLine(
            centerX - minOffset,
            centerY - minOffset,
            centerX - maxOffset,
            centerY - maxOffset, paint
        )
        canvas.drawLine(
            centerX + minOffset,
            centerY - minOffset,
            centerX + maxOffset,
            centerY - maxOffset, paint
        )
        // vertical
        canvas.drawLine(
            centerX - minOffset,
            centerY + minOffset,
            centerX - maxOffset,
            centerY + maxOffset, paint
        )
        canvas.drawLine(
            centerX + minOffset,
            centerY + minOffset,
            centerX + maxOffset,
            centerY + maxOffset, paint
        )
    }
}
