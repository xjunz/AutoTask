/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.editor

import android.util.ArrayMap
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor

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

    private val referenceRevocations = ArrayMap<AppletArg, () -> Unit>()

    private val referentRevocations = ArrayMap<AppletArg, () -> Unit>()

    private val varargReferentsRevocations = ArrayMap<Applet, () -> Unit>()

    private fun MutableMap<AppletArg, () -> Unit>.putIfNeeded(
        applet: Applet,
        which: Int,
        revocation: () -> Unit
    ) {
        if (revocable) {
            putIfAbsent(AppletArg(applet, which), revocation)
        }
    }

    private fun Applet.rawSetReferent(whichRet: Int, id: String?) {
        if (id == null) {
            rawRemoveReferent(whichRet)
        } else {
            if (referents === emptyMap<Int, String>()) {
                referents = ArrayMap()
            }
            (referents as MutableMap)[whichRet] = id
        }
    }

    private fun Applet.rawSetReference(whichArg: Int, ref: String?) {
        if (ref == null) {
            rawRemoveReference(whichArg)
        } else {
            if (references === emptyMap<Int, String>()) {
                references = ArrayMap()
            }
            (references as MutableMap)[whichArg] = ref
        }
    }

    private fun Applet.rawRemoveReference(which: Int) {
        if (references === emptyMap<Int, String>()) return
        (references as MutableMap).remove(which)
        if (references.isEmpty()) references = emptyMap()
    }

    private fun Applet.rawRemoveReferent(which: Int) {
        if (referents === emptyMap<Int, String>()) return
        (referents as MutableMap).remove(which)
        if (referents.isEmpty()) referents = emptyMap()
    }

    fun setValue(applet: Applet, whichArg: Int, value: Any?) {
        val prevValue = applet.value
        val prevReferent = applet.references[whichArg]
        applet.value = value
        applet.rawRemoveReference(whichArg)
        referenceRevocations.putIfNeeded(applet, whichArg) {
            applet.rawSetReference(whichArg, prevReferent)
            applet.value = prevValue
        }
    }

    fun setVarargReferences(applet: Applet, referentNames: List<String>) {
        val prevReferences = applet.references
        val map = ArrayMap<Int, String>()
        referentNames.forEachIndexed { index, s ->
            map[index] = s
        }
        applet.references = map
        if (revocable) {
            varargReferentsRevocations.putIfAbsent(applet) {
                applet.references = prevReferences
            }
        }
    }

    fun setReference(applet: Applet, arg: ArgumentDescriptor, whichArg: Int, referent: String?) {
        val prevReferent = applet.references[whichArg]
        val prevValue = applet.value
        applet.rawSetReference(whichArg, referent)
        // Clear its value once the arg is set to a reference
        if (!arg.isReferenceOnly) {
            applet.value = null
        }
        referenceRevocations.putIfNeeded(applet, whichArg) {
            applet.rawSetReference(whichArg, prevReferent)
            applet.value = prevValue
        }
    }

    fun renameReference(applet: Applet, whichArg: Int, newReferent: String?) {
        val prevReferent = applet.references[whichArg]
        applet.rawSetReference(whichArg, newReferent)
        referenceRevocations.putIfNeeded(applet, whichArg) {
            applet.rawSetReference(whichArg, prevReferent)
        }
    }

    fun setReferent(applet: Applet, whichResult: Int, referent: String?) {
        val prevReferent = applet.referents[whichResult]
        applet.rawSetReferent(whichResult, referent)
        referentRevocations.putIfNeeded(applet, whichResult) {
            applet.rawSetReferent(whichResult, prevReferent)
        }
    }

    fun getReferenceChangedApplets(): Set<Applet> {
        return referenceRevocations.keys.map { it.applet }.toSet()
    }

    fun reset() {
        referenceRevocations.clear()
        referentRevocations.clear()
        varargReferentsRevocations.clear()
    }

    fun revokeAll() {
        referenceRevocations.forEach { (_, u) ->
            u.invoke()
        }
        referentRevocations.forEach { (_, u) ->
            u.invoke()
        }
        varargReferentsRevocations.forEach { (_, u) ->
            u.invoke()
        }
        reset()
    }
}