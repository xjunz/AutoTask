package top.xjunz.tasker.task.applet

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.action.Action
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

fun Flow.findChildWithRefid(id: String): Applet? {
    forEach {
        if (referred.containsValue(id))
            return this

        if (it is Flow) {
            val found = it.findChildWithRefid(id)
            if (found != null)
                return found
        }
    }
    return null
}

fun Flow.requireChildWithRefid(id: String): Applet {
    return requireNotNull(findChildWithRefid(id)) {
        "Applet with refid '$id' not found!"
    }
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

/**
 * Whether the applet relates to its previous peer.
 */
inline val Applet.isRelating
    get() = index != 0 && this !is ControlFlow && this !is Action

fun <T : Applet> T.clone(factory: AppletFactory): T {
    val cloned = factory.createAppletById(id)
    cloned.id = id
    cloned.isAnd = isAnd
    cloned.value = value
    cloned.isInverted = isInverted
    cloned.isInvertible = isInvertible
    cloned.label = label
    cloned.referred = referred
    if (this is Flow) {
        cloned as Flow
        forEach {
            cloned.add(it.clone(factory))
        }
    }
    return cloned.casted()
}

fun Applet.setRefid(which: Int, tag: String) {
    if (referred === emptyMap<Int, String>()) {
        referred = mutableMapOf()
    }
    (referred as MutableMap)[which] = tag
}

fun Applet.setReference(which: Int, ref: String) {
    if (referring === emptyMap<Int, String>()) {
        referring = mutableMapOf()
    }
    (referring as MutableMap)[which] = ref
}

fun Applet.removeReference(which: Int) {
    if (referring === emptyMap<Int, String>()) return
    (referring as MutableMap).remove(which)
    if (referring.isEmpty()) referring = emptyMap()
}

fun Applet.removeRefid(which: Int) {
    if (referred === emptyMap<Int, String>()) return
    (referred as MutableMap).remove(which)
    if (referred.isEmpty()) referred = emptyMap()
}