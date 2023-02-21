/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.util

import android.util.ArrayMap
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ContainerFlow
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.factory.AppletFactory

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
 * @param shouldIntercept returns `true` to intercept the iteration.
 *
 * @return `true` if the iteration is intercepted halfway `false` otherwise
 */
fun Flow.forEachApplet(shouldIntercept: (Applet) -> Boolean): Boolean {
    forEach {
        if (shouldIntercept(it)) return true
        if (it is Flow) {
            if (it.forEachApplet(shouldIntercept)) {
                return true
            }
        }
    }
    return false
}

/**
 * Iterate all referents in a [Flow].
 *
 * @param criterion returns `true` to intercept the iteration
 * @return `true` if there is any applet meeting the [criterion], `false` otherwise
 * @see forEachApplet
 */
inline fun Flow.forEachReferent(crossinline criterion: (Applet, which: Int, referent: String) -> Boolean): Boolean {
    return forEachApplet {
        it.referents.forEach { (t, u) ->
            if (criterion(it, t, u)) return@forEachApplet true
        }
        return@forEachApplet false
    }
}

/**
 * Iterate all references in a [Flow]
 *
 * @param criterion returns `true` to intercept the iteration
 * @return `true` if there is any applet meeting the [criterion], `false` otherwise
 * @see forEachApplet
 */
inline fun Flow.forEachReference(crossinline criterion: (Applet, which: Int, referent: String) -> Boolean): Boolean {
    return forEachApplet {
        it.references.forEach { (t, u) ->
            if (criterion(it, t, u)) return@forEachApplet true
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

/**
 * @see findChildByHierarchy
 */
fun Applet.hierarchyInAncestor(ancestor: Flow?): Long {
    var hierarchy = 0L
    var p: Applet? = this
    while (p != null && p != ancestor && p.index >= 0) {
        // Note: We need index + 1, because the index starts from 0. Zero does not change
        // hierarchy value when it's left shifted.
        hierarchy = hierarchy shl Applet.FLOW_CHILD_COUNT_BITS or (p.index + 1L)
        p = p.parent
    }
    return hierarchy
}

/**
 * @see hierarchyInAncestor
 */
fun Flow.findChildByHierarchy(hierarchy: Long): Applet {
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
 * Check whether the receiver applet is executed ahead of the argument [another] at runtime.
 * If these applets are equal, returns `false`. Please make sure that these two applets have
 * the same [root] and have parents.
 */
fun Applet.isAheadOf(another: Applet): Boolean {
    if (this === another) return false
    check(this.isAttached) {
        "This [$this] must be attached to a parent flow!"
    }
    check(another.isAttached) {
        "The other [$another] must be attached to a parent flow!"
    }
    var isAhead = false
    this.root.forEachApplet {
        if (it === this) {
            isAhead = true
            true
        } else if (it === another) {
            isAhead = false
            true
        } else {
            false
        }
    }
    return isAhead
}

fun Applet.whichReferent(referent: String): Int {
    return referents.keys.firstOrNull { referents[it] == referent } ?: -1
}

fun Applet.whichReference(referent: String): Int {
    return references.keys.firstOrNull {
        references[it] == referent
    } ?: -1
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

inline val Applet.isAttached: Boolean get() = parent != null

val Applet.isEnabledInHierarchy: Boolean
    get() {
        if (!isEnabled) return false
        if (parent == null) return true
        return requireParent().isEnabledInHierarchy
    }

fun <T : Applet> T.clone(factory: AppletFactory, cloneHierarchyInfo: Boolean = true): T {
    val clone = factory.createAppletById(id)
    clone.id = id
    clone.relation = relation
    clone.isEnabled = isEnabled
    clone.value = value
    clone.isInverted = isInverted
    clone.isInvertible = isInvertible
    clone.comment = comment
    if (referents.isEmpty()) {
        clone.referents = emptyMap()
    } else {
        clone.referents = ArrayMap<Int, String>().apply {
            putAll(referents)
        }
    }
    if (references.isEmpty()) {
        clone.references = emptyMap()
    } else {
        clone.references = ArrayMap<Int, String>().apply {
            putAll(references)
        }
    }
    if (cloneHierarchyInfo) {
        clone.parent = parent
        clone.index = index
    }
    if (this is Flow) {
        clone as Flow
        forEachIndexed { index, applet ->
            val clonedChild = applet.clone(factory, false)
            clonedChild.parent = clone
            clonedChild.index = index
            clone.add(clonedChild)
        }
    }
    return clone.casted()
}