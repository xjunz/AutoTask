package top.xjunz.tasker.task.editor

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.task.applet.option.ValueDescriptor

/**
 * @author xjunz 2022/11/28
 */
class AppletReferenceEditor(private val revocable: Boolean = true) {

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

    private val referenceRevocations = mutableMapOf<AppletArg, () -> Unit>()

    private val refidRevocations = mutableMapOf<AppletArg, () -> Unit>()

    private fun MutableMap<AppletArg, () -> Unit>.putIfNeeded(
        applet: Applet,
        which: Int,
        revocation: () -> Unit
    ) {
        if (revocable) {
            putIfAbsent(AppletArg(applet, which), revocation)
        }
    }

    private fun Applet.rawSetRefid(whichRet: Int, id: String?) {
        if (id == null) {
            rawRemoveRefid(whichRet)
        } else {
            if (refids === emptyMap<Int, String>()) {
                refids = mutableMapOf()
            }
            (refids as MutableMap)[whichRet] = id
        }
    }

    private fun Applet.rawSetReference(whichArg: Int, ref: String?) {
        if (ref == null) {
            rawRemoveReference(whichArg)
        } else {
            if (references === emptyMap<Int, String>()) {
                references = mutableMapOf()
            }
            (references as MutableMap)[whichArg] = ref
        }
    }

    private fun Applet.rawRemoveReference(which: Int) {
        if (references === emptyMap<Int, String>()) return
        (references as MutableMap).remove(which)
        if (references.isEmpty()) references = emptyMap()
    }

    private fun Applet.rawRemoveRefid(which: Int) {
        if (refids === emptyMap<Int, String>()) return
        (refids as MutableMap).remove(which)
        if (refids.isEmpty()) refids = emptyMap()
    }

    fun setValue(applet: Applet, whichArg: Int, value: Any?) {
        val prevValue = applet.value
        val prevRefid = applet.references[whichArg]
        applet.value = value
        applet.rawRemoveReference(whichArg)
        referenceRevocations.putIfNeeded(applet, whichArg) {
            applet.rawSetReference(whichArg, prevRefid)
            applet.value = prevValue
        }
    }

    fun setReference(applet: Applet, arg: ValueDescriptor, whichArg: Int, refid: String?) {
        val prevRefid = applet.references[whichArg]
        val prevValue = applet.value
        applet.rawSetReference(whichArg, refid)
        // Clear its value once the arg is set to a reference
        if (!arg.isReferenceOnly) {
            applet.value = null
        }
        referenceRevocations.putIfNeeded(applet, whichArg) {
            applet.rawSetReference(whichArg, prevRefid)
            applet.value = prevValue
        }
    }

    fun renameReference(applet: Applet, whichArg: Int, newRefid: String) {
        val prevRefid = applet.references[whichArg]
        applet.rawSetReference(whichArg, newRefid)
        referenceRevocations.putIfNeeded(applet, whichArg) {
            applet.rawSetReference(whichArg, prevRefid)
        }
    }

    fun setRefid(applet: Applet, whichResult: Int, refid: String?) {
        val prevRefid = applet.refids[whichResult]
        applet.rawSetRefid(whichResult, refid)
        refidRevocations.putIfNeeded(applet, whichResult) {
            applet.rawSetRefid(whichResult, prevRefid)
        }
    }

    fun getReferenceChangedApplets(): Set<Applet> {
        return referenceRevocations.keys.map { it.applet }.toSet()
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