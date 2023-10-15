/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector

import android.graphics.Rect
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageView
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.dup
import top.xjunz.tasker.ktx.getVisibleBoundsIn
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.inspector.StableNodeInfo.Companion.freeze
import kotlin.math.hypot

/**
 * You can treat this as a snapshot of [AccessibilityNodeInfo], whose node info would never
 * become stale. Please use [AccessibilityNodeInfo.freeze] to construct an instance.
 *
 * @author xjunz 2021/9/22
 */
class StableNodeInfo constructor(val source: AccessibilityNodeInfo) {

    companion object {
        /**
         * Freeze an [AccessibilityNodeInfo] into a [StableNodeInfo] preserving its node hierarchy.
         */
        fun AccessibilityNodeInfo.freeze(): StableNodeInfo {
            val node = StableNodeInfo(dup())
            val children = ArrayList<StableNodeInfo>(childCount)
            for (i in (0 until childCount)) {
                val child = getChild(i) ?: continue
                if (!child.isVisibleToUser) continue
                // This may happen
                if (child.className == null) continue
                // Skip WebView
               // if (child.className == WebView::class.java.name) continue
                val current = child.freeze()
                current.parent = node
                current.index = children.size
                children.add(current)
            }
            if (children.isNotEmpty()) {
                children.trimToSize()
                node.children = children
            }
            return node
        }
    }

    var index: Int = -1

    var parent: StableNodeInfo? = null

    var children: List<StableNodeInfo> = emptyList()

    private var previousGlobalBounds: Rect? = null

    private lateinit var bounds: Rect

    val shortClassName: String? by lazy {
        source.className?.toString()?.substringAfterLast('.')
    }

    fun isChildOf(node: StableNodeInfo): Boolean {
        if (parent == null) return false
        if (parent == node) return true
        return parent!!.isChildOf(node)
    }

    val caption: CharSequence? by lazy {
        when {
            source.text != null -> source.text
            source.contentDescription != null -> source.contentDescription
            else -> shortClassName
        }
    }

    // TODO: Extract
    val name: String? by lazy {
        if (source.className == null) return@lazy null
        val clsName = source.className.toString()
        if (clsName.endsWith("TextView")) {
            return@lazy R.string.text.str
        }
        if (source.isScrollable
            || clsName.endsWith("RecyclerView")
            || clsName.endsWith("ListView")
            || clsName.endsWith("ScrollView")
        ) {
            return@lazy R.string.list.str
        }
        if (clsName.endsWith("ImageButton")) {
            return@lazy R.string.image_button.str
        }
        if (clsName.endsWith("SeekBar")) {
            return@lazy R.string.seekbar.str
        }
        val cls = runCatching { Class.forName(clsName) }.getOrNull()
        if (cls != null) {
            if (ImageView::class.java.isAssignableFrom(cls)) {
                return@lazy R.string.image.str
            }
            if (Button::class.java.isAssignableFrom(cls)) {
                return@lazy R.string.button.str
            }
            if (ViewGroup::class.java.isAssignableFrom(cls)) {
                return@lazy R.string.container.str
            }
        }
        return@lazy R.string.custom_view.str
    }

    fun getVisibleBoundsIn(rect: Rect): Rect {
        calculateBoundsIfNeeded(rect)
        return bounds
    }

    /**
     * Calculate node bounds inside a specific global rect. After calculated, the result is updated
     * into [bounds].
     */
    private fun calculateBoundsIfNeeded(global: Rect) {
        if (global != previousGlobalBounds) {
            bounds = source.getVisibleBoundsIn(global)
            previousGlobalBounds = global
        }
    }

    private fun containsPoint(x: Int, y: Int) = bounds.contains(x, y)

    private fun distanceToCenter(x: Int, y: Int): Float {
        return hypot(bounds.exactCenterX() - x, bounds.exactCenterY() - y)
    }

    fun findNodeUnder(x: Int, y: Int, global: Rect): StableNodeInfo? {
        calculateBoundsIfNeeded(global)
        if (!containsPoint(x, y))
            return null

        var target: StableNodeInfo? = null
        for (it in children) {
            it.calculateBoundsIfNeeded(global)
            if (!it.containsPoint(x, y))
                continue

            val candidate = it.findNodeUnder(x, y, global)
                ?: continue

            // We prefer the smaller one, because it's more likely to be concerned
            if (
                target == null
                || target.bounds.contains(candidate.bounds)
                || !candidate.bounds.contains(target.bounds)
                && target.distanceToCenter(x, y) > candidate.distanceToCenter(x, y)
            )
                target = candidate

        }
        // children don't contain the point, return itself
        if (target == null)
            return this

        return target
    }
}
