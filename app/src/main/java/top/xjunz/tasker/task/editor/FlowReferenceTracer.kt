package top.xjunz.tasker.task.editor

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.task.applet.option.ValueDescriptor
import top.xjunz.tasker.task.applet.removeReference
import top.xjunz.tasker.task.applet.setReference
import top.xjunz.tasker.task.applet.setRefid

/**
 * @author xjunz 2022/11/28
 */
class FlowReferenceTracer {

    private data class AppletArg(val applet: Applet, val which: Int) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AppletArg

            if (applet != other.applet) return false
            if (which != other.which) return false

            return true
        }

        override fun hashCode(): Int {
            var result = applet.hashCode()
            result = 31 * result + which
            return result
        }
    }

    private infix fun Applet.to(whichArg: Int): AppletArg {
        return AppletArg(this, whichArg)
    }

    private val referenceRevocations = mutableMapOf<AppletArg, () -> Unit>()

    private val refidRevocations = mutableMapOf<AppletArg, () -> Unit>()

    fun setValue(applet: Applet, whichArg: Int, value: Any?) {
        val prevValue = applet.value
        val prevRefid = applet.references[whichArg]
        applet.value = value
        applet.removeReference(whichArg)
        referenceRevocations.putIfAbsent(applet to whichArg) {
            applet.setReference(whichArg, prevRefid)
            applet.value = prevValue
        }
    }

    fun setReference(applet: Applet, arg: ValueDescriptor, whichArg: Int, refid: String?) {
        val prevRefid = applet.references[whichArg]
        val prevValue = applet.value
        applet.setReference(whichArg, refid)
        // Clear its value once the arg is set to a reference
        if (!arg.isReferenceOnly) {
            applet.value = null
        }
        referenceRevocations.putIfAbsent(applet to whichArg) {
            applet.setReference(whichArg, prevRefid)
            applet.value = prevValue
        }
    }

    fun renameReference(applet: Applet, whichArg: Int, newRefid: String) {
        val prevRefid = applet.references[whichArg]
        applet.setReference(whichArg, newRefid)
        referenceRevocations.putIfAbsent(applet to whichArg) {
            applet.setReference(whichArg, prevRefid)
        }
    }

    fun setRefid(applet: Applet, whichResult: Int, refid: String?) {
        val prevRefid = applet.refids[whichResult]
        applet.setRefid(whichResult, refid)
        refidRevocations.putIfAbsent(applet to whichResult) {
            applet.setRefid(whichResult, prevRefid)
        }
    }

    fun getReferenceChangedApplets(): Set<Applet> {
        return referenceRevocations.keys.map { it.applet }.toSet()
    }

    fun getRefidChangedApplets(): Set<Applet> {
        return refidRevocations.keys.map { it.applet }.toSet()
    }

    fun reset() {
        referenceRevocations.clear()
        refidRevocations.clear()
    }

    fun revokeAll() {
        referenceRevocations.forEach { (_, u) ->
            u.invoke()
        }
        refidRevocations.forEach { (_, u) ->
            u.invoke()
        }
        reset()
    }
}