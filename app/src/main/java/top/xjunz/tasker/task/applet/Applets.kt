/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet

import android.util.ArrayMap
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.ktx.toast
import java.util.*

/* Helper extension functions for Applet. These functions are not expected to be called in runtime.*/

/**
 * Container flow is a flow, which is only used to hold other applets.
 */
inline val Applet.isContainer: Boolean get() = this is ContainerFlow

/**
 * The nearest parent [ControlFlow].
 */
val Applet.controlFlow: ControlFlow?
    get() {
        var controlFlow: Flow? = parent
        while (controlFlow != null && controlFlow !is ControlFlow) {
            controlFlow = controlFlow.parent
        }
        return controlFlow as? ControlFlow
    }

/**
 * Find the root flow, or throw an exception when not found.
 */
val Applet.root: Flow
    get() {
        if (parent == null && this is Flow)
            return this
        return requireParent().root
    }

val Applet.rootOrNull: Flow?
    get() {
        if (parent == null)
            return if (this is Flow) this else null
        return parent?.rootOrNull
    }

val Applet.flatSize: Int
    get() = if (this is Flow) sumOf { it.flatSize } else 1

val Applet.solidSize: Int
    get() = 1 + if (this is Flow) sumOf { it.flatSize } else 0

fun Applet.isDescendantOf(flow: Flow): Boolean {
    if (parent == null) return false
    if (parent === flow)
        return true
    return requireParent().isDescendantOf(flow)
}

/**
 * @param block returns `true` to stop the iteration.
 */
fun Flow.forEachApplet(block: (Applet) -> Boolean) {
    forEach {
        if (block(it)) return
        if (it is Flow) {
            it.forEachApplet(block)
        }
    }
}

inline fun Flow.forEachReferent(crossinline block: (Applet, which: Int, referent: String) -> Boolean) {
    forEachApplet {
        it.referents.forEach { (t, u) ->
            if (block(it, t, u)) return@forEachApplet true
        }
        return@forEachApplet false
    }
}

fun Flow.buildHierarchy() {
    forEachIndexed { index, applet ->
        applet.parent = this
        applet.index = index
        if (applet is Flow)
            applet.buildHierarchy()
    }
}

inline val Applet.hierarchy: Long get() = hierarchyInAncestor(null)

fun Applet.hierarchyInAncestor(ancestor: Flow?): Long {
    var hierarchy = 0L
    var p: Applet? = this
    var d = 0
    while (p != null && p != ancestor) {
        // Note: We need index + 1, because the index starts from 0. Zero does not change
        // hierarchy value when it's left shifted.
        hierarchy = hierarchy or (p.index + 1 shl d++ * Applet.FLOW_CHILD_COUNT_BITS).toLong()
        p = p.parent
    }
    return hierarchy
}

fun Flow.requireChild(hierarchy: Long): Applet {
    var child: Applet = this
    for (d in 0 until Applet.MAX_FLOW_NESTED_DEPTH) {
        val index =
            (hierarchy ushr d * Applet.FLOW_CHILD_COUNT_BITS and Applet.MAX_FLOW_CHILD_COUNT.toLong()).toInt()
        if (index != 0) {
            child = child.casted<Flow>()[index - 1]
        } else {
            break
        }
    }
    return child
}

/**
 * Check whether the receiver applet is executed ahead of the argument [applet] at runtime.
 * If these applets are equal, returns `false`. Please make sure that these two applets have
 * the same [root].
 */
fun Applet.isAheadOf(another: Applet): Boolean {
    if (this === another) return false
    if (parent == null) return true
    if (another.parent == null) return false
    var h1 = -1
    var h2 = -1
    var i = 0
    root.forEachApplet {
        if (it === this) {
            h1 = i++
        } else if (it === another) {
            h2 = i++
        }
        h1 != -1 && h2 != -1
    }
    return h1 < h2
}

fun Applet.whichReferent(referent: String): Int {
    return referents.keys.firstOrNull { referents[it] == referent } ?: -1
}

fun Applet.whichReference(referent: String): Int {
    return references.keys.firstOrNull {
        references[it] == referent
    } ?: -1
}

inline fun Flow.forEachReference(crossinline block: (Applet, which: Int, referent: String) -> Boolean) {
    forEachApplet {
        it.references.forEach { (t, u) ->
            if (block(it, t, u)) return@forEachApplet true
        }
        return@forEachApplet false
    }
}

/**
 * The depth in root flow. If the receiver applet is the root flow, returns 0.
 *
 * @see Applet.MAX_FLOW_NESTED_DEPTH
 */
inline val Applet.depth: Int
    get() {
        var depth = 0
        var p = parent
        while (p !== null) {
            p = p.parent
            depth++
        }
        return depth
    }

/**
 * The depth in a specific flow. If the receiver applet is the flow, returns 0. If the receiver flow
 * is not a descendant of the fow, returns -1.
 */
fun Applet.depthInAncestor(flow: Flow): Int {
    var depth = 0
    var p: Applet? = this
    while (p !== flow) {
        // Not its descendant
        if (p == null) return -1
        p = p.parent
        depth++
    }
    return depth
}

inline val Applet.isAttached: Boolean get() = rootOrNull is RootFlow

val Applet.isEnabledInHierarchy: Boolean
    get() {
        if (!isEnabled) return false
        if (parent == null) return true
        return requireParent().isEnabledInHierarchy
    }

fun <T : Applet> T.clone(factory: AppletFactory, cloneHierarchyInfo: Boolean = true): T {
    val cloned = factory.createAppletById(id)
    cloned.id = id
    cloned.isAnd = isAnd
    cloned.isEnabled = isEnabled
    cloned.value = value
    cloned.isInverted = isInverted
    cloned.isInvertible = isInvertible
    cloned.comment = comment
    cloned.referents = ArrayMap<Int, String>().apply {
        putAll(referents)
    }
    cloned.references = ArrayMap<Int, String>().apply {
        putAll(references)
    }
    if (cloneHierarchyInfo) {
        cloned.parent = parent
        cloned.index = index
    }
    if (this is Flow) {
        cloned as Flow
        forEachIndexed { index, applet ->
            val clonedChild = applet.clone(factory, false)
            clonedChild.parent = cloned
            clonedChild.index = index
            cloned.add(clonedChild)
        }
    }
    return cloned.casted()
}

fun Flow.addSafely(applet: Applet): Boolean {
    return addAllSafely(Collections.singleton(applet))
}

fun Flow.addAllSafely(applets: Collection<Applet>): Boolean {
    if (size + applets.size <= maxSize) {
        addAll(applets)
        return true
    }
    toast(R.string.error_over_max_applet_size)
    return false
}