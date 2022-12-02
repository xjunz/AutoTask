package top.xjunz.tasker.task.applet

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.factory.AppletFactory

/* Helper extension functions for Applets. These functions is not expected to be called in runtime.*/

/**
 * Container flow is a flow, which is only used to hold other applets.
 */
inline val Applet.isContainer: Boolean get() = javaClass == Flow::class.java

/**
 * The nearest non-container parent flow.
 */
val Applet.nonContainerParent: Flow?
    get() {
        if (parent == null) return null
        if (requireParent().isContainer) return requireParent().nonContainerParent
        return requireParent()
    }

/**
 * The nearest parent [ControlFlow].
 */
val Applet.controlFlowParent: ControlFlow?
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

val Flow.flatSize: Int
    get() {
        var size = 0
        forEach {
            if (it is Flow) {
                size += it.flatSize
            } else {
                size++
            }
        }
        return size
    }

fun Applet.isDescendantOf(flow: Flow): Boolean {
    if (parent == null) return false
    if (parent === flow)
        return true
    return requireParent().isDescendantOf(flow)
}


private fun Flow.findChildrenReferringRefidRecur(ret: MutableMap<Applet, Int>, refid: String) {
    forEach {
        for ((which, id) in it.references) {
            if (id == refid) {
                ret[it] = which
                break
            }
        }
        if (it is Flow) {
            it.findChildrenReferringRefidRecur(ret, refid)
        }
    }
}

fun Flow.findChildrenReferringRefid(refid: String): Map<Applet, Int> {
    val ret = mutableMapOf<Applet, Int>()
    findChildrenReferringRefidRecur(ret, refid)
    return if (ret.isEmpty()) emptyMap() else ret
}

fun Flow.hasChildOwningRefid(id: String): Boolean {
    forEach {
        if (refids.containsValue(id))
            return true

        if (it is Flow) {
            val found = it.hasChildOwningRefid(id)
            if (found)
                return true
        }
    }
    return false
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

private val Applet.hierarchy: Int
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
    return references.keys.firstOrNull { references[it] == refid } ?: -1
}

inline fun Flow.forEachReference(crossinline block: (Applet, which: Int, refid: String) -> Boolean) {
    iterate {
        it.references.forEach { (t, u) ->
            if (block(it, t, u)) return@iterate true
        }
        return@iterate false
    }
}

fun Flow.findChildOwningRefid(id: String): List<Applet> {
    val ret = mutableListOf<Applet>()
    forEach {
        if (it.refids.containsValue(id))
            ret.add(it)
        if (it is Flow) {
            ret.addAll(it.findChildOwningRefid(id))
        }
    }
    return if (ret.isEmpty()) emptyList() else ret
}

fun Flow.findAnyChildOwningRefid(id: String): Applet? {
    forEach {
        if (it.refids.containsValue(id))
            return it
        if (it is Flow) {
            return it.findAnyChildOwningRefid(id)
        }
    }
    return null
}

/**
 * The depth in root flow, starting from 0, which means the nested depth in root flow whose parent
 * is `null`.
 *
 * @see Applet.MAX_FLOW_NESTED_DEPTH
 */
inline val Applet.depth: Int
    get() {
        var depth = 0
        var p = parent
        while (p != null) {
            p = p.parent
            depth++
        }
        return depth
    }

fun <T : Applet> T.clone(factory: AppletFactory, cloneHierarchyInfo: Boolean = true): T {
    val cloned = factory.createAppletById(id)
    cloned.id = id
    cloned.isAnd = isAnd
    cloned.value = value
    cloned.isInverted = isInverted
    cloned.isInvertible = isInvertible
    cloned.label = label
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