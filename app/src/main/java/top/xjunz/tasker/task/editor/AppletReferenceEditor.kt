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

    private fun Map<Int, String>.putIfDistinct(key: Int, value: String): Boolean {
        if (this[key] == value) return false
        (this as MutableMap<Int, String>)[key] = value
        return true
    }

    private fun Applet.rawSetReferent(whichRet: Int, id: String?): Boolean {
        return if (id.isNullOrEmpty()) {
            rawRemoveReferent(whichRet)
        } else {
            if (referents === emptyMap<Int, String>()) {
                referents = ArrayMap()
            }
            referents.putIfDistinct(whichRet, id)
        }
    }

    private fun Applet.rawSetReference(whichArg: Int, ref: String?): Boolean {
        return if (ref.isNullOrEmpty()) {
            rawRemoveReference(whichArg)
        } else {
            if (references === emptyMap<Int, String>()) {
                references = ArrayMap()
            }
            references.putIfDistinct(whichArg, ref)
        }
    }

    private fun Applet.rawRemoveReference(which: Int): Boolean {
        if (references.isEmpty()) return false
        (references as MutableMap).remove(which)
        if (references.isEmpty()) references = emptyMap()
        return true
    }

    private fun Applet.rawRemoveReferent(which: Int): Boolean {
        if (referents.isEmpty()) return false
        (referents as MutableMap).remove(which)
        if (referents.isEmpty()) referents = emptyMap()
        return true
    }

    fun setValue(applet: Applet, whichArg: Int, value: Any?) {
        val prevValue = applet.value
        val prevReferent = applet.references[whichArg]
        applet.value = value
        if (applet.rawRemoveReference(whichArg)) {
            referenceRevocations.putIfNeeded(applet, whichArg) {
                applet.rawSetReference(whichArg, prevReferent)
                applet.value = prevValue
            }
        }
    }

    fun setVarargReferences(applet: Applet, referentNames: List<String>) {
        val prevReferences = applet.references
        val map = ArrayMap<Int, String>()
        var changed = false
        referentNames.forEachIndexed { index, s ->
            if (map[index] != s) {
                map[index] = s
                changed = true
            }
        }
        if (changed) {
            applet.references = map
            if (revocable) {
                varargReferentsRevocations.putIfAbsent(applet) {
                    applet.references = prevReferences
                }
            }
        }
    }

    fun setReference(applet: Applet, arg: ArgumentDescriptor, whichArg: Int, referent: String?) {
        val prevReferent = applet.references[whichArg]
        val prevValue = applet.value
        if (applet.rawSetReference(whichArg, referent)) {
            // Clear its value once the arg is set to a reference
            if (!arg.isReferenceOnly) {
                applet.value = null
            }
            referenceRevocations.putIfNeeded(applet, whichArg) {
                applet.rawSetReference(whichArg, prevReferent)
                applet.value = prevValue
            }
        }
    }

    fun renameReference(applet: Applet, whichArg: Int, newReferent: String?) {
        val prevReferent = applet.references[whichArg]
        if (applet.rawSetReference(whichArg, newReferent)) {
            referenceRevocations.putIfNeeded(applet, whichArg) {
                applet.rawSetReference(whichArg, prevReferent)
            }
        }
    }

    fun setReferent(applet: Applet, whichResult: Int, referent: String?) {
        val prevReferent = applet.referents[whichResult]
        if (applet.rawSetReferent(whichResult, referent)) {
            referentRevocations.putIfNeeded(applet, whichResult) {
                applet.rawSetReferent(whichResult, prevReferent)
            }
        }
    }

    fun getReferenceChangedApplets(): Set<Applet> {
        return referenceRevocations.keys.map { it.applet }.toSet()
    }

    fun getReferentChangedApplets(): Set<Applet> {
        return referentRevocations.keys.map { it.applet }.toSet()
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