package top.xjunz.tasker.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.task.core.StableNode
import kotlin.math.hypot


/**
 * @author xjunz 2021/9/17
 */
class LayoutInspectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var rootNode: StableNode? = null

    /**
     * The node which is currently being touched or being focused.
     */
    var emphaticNode: StableNode? = null

    private val boundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.dpFloat
        color = Color.RED
        isAntiAlias = true
    }

    private val emphaticPaint = Paint(boundsPaint).apply {
        color = Color.YELLOW
        strokeWidth = 3.dpFloat
    }

    private var actionDownX = 0F

    private var actionDownY = 0F

    private var isOverSlop = false

    /**
     * The listener to be notified when a node is selected by the user.
     */
    var onNodeSelectedListener: ((StableNode) -> Unit)? = null

    private fun requireRootNode() = requireNotNull(rootNode) { "The root node is not set!" }

    private fun requireEmphaticNode() =
        requireNotNull(emphaticNode) { "There is no emphatic node!" }

    fun clearNode() {
        rootNode = null
        emphaticNode = null
    }

    fun setRootNode(node: StableNode) {
        rootNode = node
        emphaticNode = null
        invalidate()
    }

    fun findNextNode(): Boolean {
        emphaticNode?.next?.let {
            emphaticNode = it
            invalidate()
            return true
        }
        emphaticNode?.child?.let {
            emphaticNode = it
            invalidate()
            return true
        }
        return false
    }

    fun findPreviousNode(): Boolean {
        emphaticNode?.prev?.let {
            emphaticNode = it
            invalidate()
            return true
        }
        emphaticNode?.parent?.let {
            emphaticNode = it
            invalidate()
            return true
        }
        return false
    }

    fun findParentNode(): Boolean {
        emphaticNode?.parent?.let {
            emphaticNode = it
            invalidate()
            return true
        }
        return false
    }

    fun findChildNode(): Boolean {
        emphaticNode?.child?.let {
            emphaticNode = it
            invalidate()
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> findParentNode()
            KeyEvent.KEYCODE_DPAD_DOWN -> findChildNode()
            KeyEvent.KEYCODE_DPAD_LEFT -> findPreviousNode()
            KeyEvent.KEYCODE_DPAD_RIGHT -> findNextNode()
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun drawEmphaticBounds(canvas: Canvas, node: StableNode) {
        canvas.drawRect(node.bounds, emphaticPaint)
    }

    private fun drawNormalNode(canvas: Canvas, node: StableNode) {
        var child = node.child
        while (child != null) {
            canvas.drawRect(child.bounds, boundsPaint)
            drawNormalNode(canvas, child)
            child = child.next
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rootNode != null ) {
            drawNormalNode(canvas, requireRootNode())
        }
        if (emphaticNode != null) {
            drawEmphaticBounds(canvas, requireEmphaticNode())
        }
    }

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun performClick(): Boolean {
        emphaticNode?.let {
            onNodeSelectedListener?.invoke(it)
        }
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (rootNode == null) return super.onTouchEvent(event)
        val x = event.x
        val y = event.y
        emphaticNode = requireRootNode().findNodeUnder(x.toInt(), y.toInt())
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


}
