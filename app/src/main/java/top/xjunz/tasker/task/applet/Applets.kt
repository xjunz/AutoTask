package top.xjunz.tasker.task.applet

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
 * The nearest non-container parent of this applet, may be itself.
 */
val Applet.scopeFlow: Flow
    get() {
        if (this is Flow && !isContainer) return this
        return requireParent().scopeFlow
    }

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

fun Flow.iterate(block: (Applet) -> Boolean) {
    forEach {
        if (block(it)) return@forEach
        if (it is Flow) {
            it.iterate(block)
        }
    }
}

inline fun Flow.forEachRefid(crossinline block: (Applet, which: Int, refid: String) -> Boolean) {
    iterate {
        it.refids.forEach { (t, u) ->
            if (block(it, t, u)) return@iterate true
        }
        return@iterate false
    }
}

private inline val Applet.hierarchy: Int
    get() {
        var hierarchy = 0
        var p: Applet? = this
        var d = 0
        while (p != null) {
            // Note: We need index + 1, because the index starts from 0. Zero does not change
            // hierarchy value when it's left shifted.
            hierarchy = hierarchy or (p.index + 1 shl d++ * Applet.FLOW_CHILD_COUNT_BITS)
            p = p.parent
        }
        return hierarchy
    }

fun Applet.isAheadOf(applet: Applet): Boolean {
    return hierarchy < applet.hierarchy
}

fun Applet.whichRefid(refid: String): Int {
    return refids.keys.firstOrNull { refids[it] == refid } ?: -1
}

fun Applet.whichReference(refid: String): Int {
    return references.keys.firstOrNull {
        references[it] == refid
    } ?: -1
}

inline fun Flow.forEachReference(crossinline block: (Applet, which: Int, refid: String) -> Boolean) {
    iterate {
        it.references.forEach { (t, u) ->
            if (block(it, t, u)) return@iterate true
        }
        return@iterate false
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
    cloned.refids = mutableMapOf<Int, String>().apply {
        putAll(refids)
    }
    cloned.references = mutableMapOf<Int, String>().apply {
        putAll(references)
    }
    if (cloneHierarchyInfo) {
        cloned.parent = parent
        cloned.index = index
    }
    if (this is Flow) {
        cloned as Flow
        forEach {
            cloned.add(it.clone(factory))
        }
    }
    return cloned.casted()
}

fun Applet.setRefid(whichRet: Int, id: String?) {
    if (id == null) {
        removeRefid(whichRet)
    } else {
        if (refids === emptyMap<Int, String>()) {
            refids = mutableMapOf()
        }
        (refids as MutableMap)[whichRet] = id
    }
}

fun Applet.setReference(whichArg: Int, ref: String?) {
    if (ref == null) {
        removeReference(whichArg)
    } else {
        if (references === emptyMap<Int, String>()) {
            references = mutableMapOf()
        }
        (references as MutableMap)[whichArg] = ref
    }
}

fun Applet.removeReference(which: Int) {
    if (references === emptyMap<Int, String>()) return
    (references as MutableMap).remove(which)
    if (references.isEmpty()) references = emptyMap()
}

fun Applet.removeRefid(which: Int) {
    if (refids === emptyMap<Int, String>()) return
    (refids as MutableMap).remove(which)
    if (refids.isEmpty()) refids = emptyMap()
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