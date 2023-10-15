/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.editor

import android.util.ArrayMap
import top.xjunz.tasker.engine.applet.base.Applet

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

    private val valueRevocations = ArrayMap<AppletArg, () -> Unit>()

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

    private fun <V> Map<Int, V>.putIfDistinct(key: Int, value: V): Boolean {
        if (this[key] == value) return false
        (this as MutableMap<Int, V>)[key] = value
        return true
    }

    private fun Applet.rawSetValue(which: Int, value: Any?): Boolean {
        return if (value == null) {
            rawRemoveValue(which)
        } else {
            if (values === emptyMap<Int, Any>()) {
                values = ArrayMap(1)
            }
            values.putIfDistinct(which, value)
        }
    }

    private fun Applet.rawRemoveValue(which: Int): Boolean {
        if (values.isEmpty()) return false
        if (!values.containsKey(which)) return false
        (values as MutableMap).remove(which)
        if (values.isEmpty()) values = emptyMap()
        return true
    }

    private fun Applet.rawSetReferent(whichRet: Int, id: String?): Boolean {
        return if (id.isNullOrEmpty()) {
            rawRemoveReferent(whichRet)
        } else {
            if (referents === emptyMap<Int, String>()) {
                referents = ArrayMap(1)
            }
            referents.putIfDistinct(whichRet, id)
        }
    }

    private fun Applet.rawSetReference(whichArg: Int, ref: String?): Boolean {
        return if (ref.isNullOrEmpty()) {
            rawRemoveReference(whichArg)
        } else {
            if (references === emptyMap<Int, String>()) {
                references = ArrayMap(1)
            }
            references.putIfDistinct(whichArg, ref)
        }
    }

    private fun Applet.rawRemoveReference(which: Int): Boolean {
        if (!references.containsKey(which)) return false
        (references as MutableMap).remove(which)
        if (references.isEmpty()) references = emptyMap()
        return true
    }

    private fun Applet.rawRemoveReferent(which: Int): Boolean {
        if (!referents.containsKey(which)) return false
        (referents as MutableMap).remove(which)
        if (referents.isEmpty()) referents = emptyMap()
        return true
    }

    private fun ensureArgumentNotDuplicated(applet: Applet, which: Int) {
        check(!(applet.values.containsKey(which) && applet.references.containsKey(which))) {
            "Argument $which is defined in both values and references!"
        }
    }

    fun setValue(applet: Applet, which: Int, value: Any?) {
        ensureArgumentNotDuplicated(applet, which)
        val prevValue = applet.values[which]
        val prevRef = applet.references[which]
        if (applet.rawSetValue(which, value)) {
            valueRevocations.putIfNeeded(applet, which) {
                applet.rawSetValue(which, prevValue)
            }
        }
        if (applet.rawRemoveReference(which)) {
            referenceRevocations.putIfNeeded(applet, which) {
                applet.rawSetReference(which, prevRef)
            }
        }
    }

    fun setReference(applet: Applet, which: Int, ref: String?) {
        ensureArgumentNotDuplicated(applet, which)
        val prevRef = applet.references[which]
        val prevValue = applet.values[which]
        if (applet.rawRemoveValue(which)) {
            valueRevocations.putIfNeeded(applet, which) {
                applet.rawSetValue(which, prevValue)
            }
        }
        if (applet.rawSetReference(which, ref)) {
            referenceRevocations.putIfNeeded(applet, which) {
                applet.rawSetReference(which, prevRef)
            }
        }
    }

    fun setVarargReferences(applet: Applet, refNames: List<String>) {
        val prevReferences = applet.references
        val map = ArrayMap<Int, String>()
        var changed = false
        refNames.forEachIndexed { index, s ->
            // started from 1, 0 is preserved for value
            // because value and reference cannot both be defined at index 0
            map[index + 1] = s
            if (!changed && s != prevReferences[index + 1]) {
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

    fun renameReference(applet: Applet, which: Int, newName: String?) {
        val prevName = requireNotNull(applet.references[which])
        if (applet.rawSetReference(which, newName)) {
            referenceRevocations.putIfNeeded(applet, which) {
                applet.rawSetReference(which, prevName)
            }
        }
    }

    fun setReferent(applet: Applet, which: Int, referent: String?) {
        val prevReferent = applet.referents[which]
        if (applet.rawSetReferent(which, referent)) {
            referentRevocations.putIfNeeded(applet, which) {
                applet.rawSetReferent(which, prevReferent)
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
        valueRevocations.clear()
        varargReferentsRevocations.clear()
    }

    fun revokeAll() {
        valueRevocations.forEach { (_, u) ->
            u.invoke()
        }
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