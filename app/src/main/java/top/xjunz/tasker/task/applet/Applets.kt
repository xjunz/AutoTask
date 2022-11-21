package top.xjunz.tasker.task.applet

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.factory.AppletFactory

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
    cloned.refid = refid
    if (this is Flow) {
        cloned as Flow
        forEach {
            cloned.add(it.clone(factory))
        }
    }
    return cloned.casted()
}
