package top.xjunz.tasker.ui.task.inspector

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.*
import android.view.animation.OvershootInterpolator
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.math.MathUtils
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.databinding.OverlayBubbleCollapsedBinding
import top.xjunz.tasker.databinding.OverlayBubbleExpandedBinding
import top.xjunz.tasker.databinding.OverlayInspectorBinding
import top.xjunz.tasker.databinding.OverlayTrashBinBinding
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.task.core.StableNode
import top.xjunz.tasker.trace.logcat
import top.xjunz.tasker.ui.widget.BubbleLayout
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout
import top.xjunz.tasker.ui.widget.LayoutInspectorView

/**
 * @author xjunz 2021/9/21
 */
class FloatingInspector {

    var isShown: Boolean = false

    private val appContext = ContextThemeWrapper(app, R.style.AppTheme)

    private val windowManager by lazy {
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val expandedBubbleLps by lazy {
        WindowManager.LayoutParams().apply {
            copyFrom(inspectorLps)
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = width
        }
    }

    private val collapsedBubbleLps by lazy {
        WindowManager.LayoutParams().apply { copyFrom(expandedBubbleLps) }
    }

    private val trashBinLps by lazy {
        WindowManager.LayoutParams().apply {
            copyFrom(collapsedBubbleLps)
            width = appContext.resources.displayMetrics.widthPixels
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
    }

    private val widthDetectorLps by lazy {
        WindowManager.LayoutParams().apply {
            copyFrom(collapsedBubbleLps)
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = 1
            gravity = Gravity.TOP
        }
    }

    private val heightDetectorLps by lazy {
        WindowManager.LayoutParams().apply {
            copyFrom(collapsedBubbleLps)
            width = 1
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.START
        }
    }

    private var isPortrait = true
    private var windowWidth = 0
    private var windowHeight = 0

    private val widthDetector by lazy {
        View(appContext).apply {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                windowWidth = width
                if (windowHeight != windowWidth) isPortrait = windowHeight > windowWidth
            }
        }
    }

    private val heightDetector by lazy {
        View(appContext).apply {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                windowHeight = height
                if (windowHeight != windowWidth) isPortrait = windowHeight > windowWidth
            }
        }
    }

    @Suppress("DEPRECATION")
    private val inspectorLps by lazy {
        WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else
                WindowManager.LayoutParams.TYPE_PHONE
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
    }

    private val desaturatedColorFilter by lazy {
        ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0F) })
    }

    private fun isInTrashBin(): Boolean {
        return collapsedBubbleLps.y - collapsedBubble.height / 2 >=
                trashBinLps.y - trashBinOverlay.height / 2
                && collapsedBubbleLps.x - collapsedBubble.width / 2 >=
                trashBinLps.x - trashBinOverlay.width / 2
                && collapsedBubbleLps.x + collapsedBubble.width / 2 <=
                trashBinLps.x + trashBinOverlay.width / 2
    }

    private lateinit var collapsedBubble: View

    private val collapsedBubbleBinding by lazy {
        OverlayBubbleCollapsedBinding.inflate(LayoutInflater.from(appContext)).apply {
            collapsedBubble = root
            bubbleWrapper.onDragListener = { state, offsetX, offsetY ->
                when (state) {
                    FloatingDraggableLayout.STATE_DRAG_STARTED -> {
                        trashBinOverlay.isVisible = true
                        trashBinOverlay.doOnPreDraw {
                            trashBinLps.y = windowHeight / 2 - trashBinOverlay.height / 2
                            windowManager.updateViewLayout(trashBinOverlay, trashBinLps)
                            trashBinOverlay.animate().translationX(0F)
                                .setInterpolator(FastOutSlowInInterpolator()).start()
                        }
                    }
                    FloatingDraggableLayout.STATE_DRAGGING -> {
                        offsetViewInWindow(
                            offsetX.toInt(), offsetY.toInt(), collapsedBubbleLps, root
                        )
                        if (isInTrashBin()) {
                            root.background.colorFilter = desaturatedColorFilter
                            ibCenter.colorFilter = desaturatedColorFilter
                        } else {
                            root.background.colorFilter = null
                            ibCenter.colorFilter = null
                        }
                    }
                    FloatingDraggableLayout.STATE_DRAG_ENDED -> {
                        if (isInTrashBin()) {
                            ObjectAnimator.ofFloat(
                                collapsedBubble, View.TRANSLATION_X, trashBinOverlay.width.toFloat()
                            ).apply {
                                interpolator = FastOutSlowInInterpolator()
                                doOnEnd {
                                    collapsedBubble.isVisible = false
                                    collapsedBubble.translationX = 0F
                                    root.background.colorFilter = null
                                    ibCenter.colorFilter = null
                                    collapsedBubbleLps.x = 0
                                    collapsedBubbleLps.y = 0
                                    dismiss()
                                }
                            }.start()
                        }
                        ObjectAnimator.ofFloat(
                            trashBinOverlay, View.TRANSLATION_X, trashBinOverlay.width.toFloat()
                        ).apply {
                            interpolator = FastOutSlowInInterpolator()
                            doOnEnd { trashBinOverlay.isVisible = false }
                        }.start()
                    }
                }
            }
            ibCenter.setOnClickListener {
                toggleInspector()
            }
        }
    }

    private fun offsetViewInWindow(
        offsetX: Int, offsetY: Int, lp: WindowManager.LayoutParams, v: View
    ) {
        lp.x = MathUtils.clamp(
            lp.x + offsetX, v.width / 2 - windowWidth / 2, windowWidth / 2 - v.width / 2
        )
        lp.y = MathUtils.clamp(
            lp.y + offsetY, v.height / 2 - windowHeight / 2, windowHeight / 2 - v.height / 2
        )
        windowManager.updateViewLayout(v, lp)
    }

    private val expandedBubbleBinding by lazy {
        OverlayBubbleExpandedBinding.inflate(LayoutInflater.from(appContext)).apply {
            expandedBubble = root as BubbleLayout
            bubbleWrapper.onDragListener = { offsetX, offsetY ->
                offsetViewInWindow(offsetX.toInt(), offsetY.toInt(), expandedBubbleLps, root)
            }
            ibCenter.setOnClickListener { toggleInspector() }
            ibBottom.setOnClickListener {
                inspector.findChildNode()
                if (inspector.emphaticNode != null) {
                    nodeAdapter.setNode(inspector.emphaticNode!!)
                }
            }
            ibLeft.setOnClickListener {
                inspector.findPreviousNode()
                if (inspector.emphaticNode != null) {
                    nodeAdapter.setNode(inspector.emphaticNode!!)
                }
            }
            ibRight.setOnClickListener {
                inspector.findNextNode()
                if (inspector.emphaticNode != null) {
                    nodeAdapter.setNode(inspector.emphaticNode!!)
                }
            }
            ibTop.setOnClickListener {
                inspector.findParentNode()
                if (inspector.emphaticNode != null) {
                    nodeAdapter.setNode(inspector.emphaticNode!!)
                }
            }
            root.doOnPreDraw {
                it.isVisible = false
            }
        }
    }

    private lateinit var trashBinOverlay: View

    private val trashBinBinding by lazy {
        OverlayTrashBinBinding.inflate(LayoutInflater.from(appContext)).apply {
            trashBinOverlay = root
            root.doOnPreDraw {
                it.isVisible = false
                it.translationX = it.width.toFloat()
            }
        }
    }

    private val nodeAdapter = NodeAdapter()

    private lateinit var expandedBubble: BubbleLayout
    private lateinit var inspectorOverlay: View
    private lateinit var inspector: LayoutInspectorView

    private val inspectorBinding by lazy {
        OverlayInspectorBinding.inflate(LayoutInflater.from(appContext)).apply {
            inspectorOverlay = root
            inspector = inspectorView
            btnSelect.setOnClickListener {
                // nodePanel.isVisible = false
            }
            btnReselect.setOnClickListener {
            }

            btnDismiss.setOnClickListener {
                nodePanel.isVisible = false
            }
            rvAttrs.adapter = nodeAdapter
            inspectorView.onNodeSelectedListener = {
                nodePanel.isVisible = true
                nodeAdapter.setNode(it)
            }
        }
    }

    private fun expandBubble() {
        expandedBubble.isVisible = true
        // adjust the expanded bubble's position to the collapsed bubble's
        expandedBubbleLps.x = collapsedBubbleLps.x
        expandedBubbleLps.y = collapsedBubbleLps.y
        windowManager.updateViewLayout(expandedBubble, expandedBubbleLps)
        expandedBubble.doOnPreDraw {
            it.animate().alpha(1F).setInterpolator(null).start()
            collapsedBubble.animate().alpha(0F).setInterpolator(OvershootInterpolator()).start()
            ViewAnimationUtils.createCircularReveal(
                it, it.width / 2, it.height / 2,
                collapsedBubble.width / 2f, it.width / 2f
            ).apply {
                interpolator = FastOutSlowInInterpolator()
                addListener(onEnd = { collapsedBubble.isVisible = false })
            }.start()
        }
    }

    private fun isRestricted(): Boolean {
        collapsedBubbleLps.run {
            val radius = expandedBubble.width / 2
            val ax = x + windowWidth / 2
            val ay = y + windowHeight / 2
            return ax < radius || windowWidth - ax < radius || ay < radius || windowHeight - ay < radius
        }
    }

    private fun collapseBubble() {
        collapsedBubbleLps.x = expandedBubbleLps.x
        collapsedBubbleLps.y = expandedBubbleLps.y
        windowManager.updateViewLayout(collapsedBubble, collapsedBubbleLps)
        collapsedBubble.run {
            isVisible = true
            animate().alpha(1F).setInterpolator(null).setListener(null).start()
        }
        expandedBubble.run {
            if (isRestricted()) {
                animate().setInterpolator(OvershootInterpolator()).alpha(0F)
            }
            ViewAnimationUtils.createCircularReveal(
                this, width / 2, height / 2,
                width / 2f, collapsedBubble.width / 2f - collapsedBubble.elevation
            ).also {
                it.interpolator = FastOutSlowInInterpolator()
                it.addListener(onEnd = { isVisible = false })
            }.start()
        }
    }

    private fun showWindowNodeBounds() {
        val rootNode = A11yAutomatorService.require().rootInActiveWindow
        if (rootNode == null) {
            logcat("Got null root node!")
            return
        }
        val visibleBounds = Rect()
        inspector.getWindowVisibleDisplayFrame(visibleBounds)
        StableNode.freeze(rootNode, visibleBounds)?.let {
            inspector.setRootNode(it)
        }
    }

    private fun toggleInspector() {
        // animating, don't bother!
        if (collapsedBubble.alpha != 1F && collapsedBubble.alpha != 0F) {
            return
        }
        expandedBubbleBinding.run {
            if (inspectorOverlay.isVisible) {
                collapseBubble()
                inspector.clearNode()
                inspectorOverlay.isVisible = false
                inspectorBinding.nodePanel.isVisible = false
            } else {
                inspectorOverlay.isVisible = true
                showWindowNodeBounds()
                expandBubble()
            }
        }
    }

    fun show() {
        windowManager.addView(widthDetector, widthDetectorLps)
        windowManager.addView(heightDetector, heightDetectorLps)
        windowManager.addView(inspectorBinding.root, inspectorLps)
        windowManager.addView(expandedBubbleBinding.root, expandedBubbleLps)
        windowManager.addView(trashBinBinding.root, trashBinLps)
        windowManager.addView(collapsedBubbleBinding.root, collapsedBubbleLps)
        collapsedBubbleBinding.root.isVisible = true
        collapsedBubbleBinding.root.doOnPreDraw {
            it.scaleX = 0F
            it.scaleY = 0F
            it.animate().scaleX(1F).scaleY(1F).setInterpolator(OvershootInterpolator()).start()
        }
        isShown = true
    }

    fun dismiss() {
        windowManager.removeView(widthDetector)
        windowManager.removeView(heightDetector)
        windowManager.removeView(inspectorOverlay)
        windowManager.removeView(expandedBubble)
        windowManager.removeView(trashBinOverlay)
        windowManager.removeView(collapsedBubble)
        isShown = false
    }
}
