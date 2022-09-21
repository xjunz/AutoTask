package top.xjunz.tasker.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import kotlin.math.hypot


/**
 * @author xjunz 2021/9/24
 */
class BubbleLayout @JvmOverloads constructor(
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

    private var downPoint = FloatArray(2)

    private var currentPoint = FloatArray(2)

    private var capturedView: View? = null

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    var onDragListener: ((offsetX: Float, offsetY: Float) -> Unit)? = null

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
                        left,
                        measuredHeight / 2 - child.measuredHeight / 2,
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
                        measuredWidth / 2 - child.measuredWidth / 2,
                        top,
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

    private fun findTopChildUnder(x: Float, y: Float): View? {
        val childCount: Int = childCount
        for (i in childCount - 1 downTo 0) {
            val child: View = getChildAt(i)
            if (x >= child.left && x < child.right && y >= child.top && y < child.bottom) {
                return child
            }
        }
        return null
    }

    /**
     * Intercept the touch event when the child underneath is not going to handle the event, i.e. its
     * [View.onTouchEvent] returns false, or when the [MotionEvent] has moved an as enough long distance
     * as the [touchSlop]. When the touch event is intercepted, we will drag this view as the finger
     * moves.
     *
     * @see onTouchEvent
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val rawX = ev.rawX
        val rawY = ev.rawY
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downPoint[0] = rawX
                downPoint[1] = rawY
                currentPoint[0] = rawX
                currentPoint[1] = rawY
                findTopChildUnder(ev.x, ev.y)?.let {
                    if (it.onTouchEvent(ev)) {
                        // captured an clickable child, let it handle the event temporarily
                        capturedView = it
                        return false
                    }
                }
                // otherwise let me handle the event
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val view = findTopChildUnder(ev.x, ev.y)
                if (capturedView != null && capturedView == view) {
                    if (hypot(rawX - downPoint[0], rawY - downPoint[1]) < touchSlop) {
                        return false
                    }
                }
                // start drag myself
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                onDragListener?.invoke(rawX - currentPoint[0], rawY - currentPoint[1])
                currentPoint[0] = rawX
                currentPoint[1] = rawY
            }
            MotionEvent.ACTION_UP -> capturedView = null
        }
        return true
    }
}
