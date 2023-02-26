/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.applet.util.forEachReference
import top.xjunz.tasker.engine.applet.util.forEachReferent
import top.xjunz.tasker.engine.applet.util.isAheadOf
import top.xjunz.tasker.engine.applet.util.isAttached
import top.xjunz.tasker.engine.task.TaskSnapshot
import top.xjunz.tasker.task.editor.AppletReferenceEditor

/**
 * A global view model serving all [FlowEditorDialog]s, this view model is expected to
 * be hosted in a [FlowEditorViewModel] whose [FlowEditorViewModel.isBase] is true.
 *
 * @author xjunz 2022/11/26
 */
class GlobalFlowEditorViewModel : ViewModel() {

    lateinit var root: RootFlow

    private val _selectedReferents = mutableSetOf<Pair<Applet, Int>>()

    val selectedReferents: Set<Pair<Applet, Int>> get() = _selectedReferents

    val onReferentSelected = MutableLiveData<Boolean>()

    val onAppletChanged = MutableLiveData<Applet?>()

    val referenceEditor = AppletReferenceEditor()

    val currentSnapshotIndex = MutableLiveData<Int?>()

    val allSnapshots = MutableLiveData<Array<TaskSnapshot>?>()

    var currentSnapshot: TaskSnapshot? = null
        set(value) {
            value?.loadApplets(root)
            field = value
        }

    val onSnapshotsCleared = MutableLiveData<Boolean>()

    fun renameReferentInRoot(prev: Set<String>, cur: String?) {
        root.forEachReferent { applet, which, referent ->
            if (prev.contains(referent)) {
                referenceEditor.setReferent(applet, which, cur)
                true
            } else {
                false
            }
        }
        root.forEachReference { applet, which, referent ->
            if (prev.contains(referent)) {
                referenceEditor.renameReference(applet, which, cur)
            }
            false
        }
    }

    fun getSelectedReferentNames(): List<String> {
        return selectedReferents.mapNotNull { (applet, which) ->
            applet.referents[which]
        }
    }

    fun isReferentSelected(applet: Applet, which: Int): Boolean {
        return _selectedReferents.any {
            it.first === applet && it.second == which
        }
    }

    fun isReferentSelected(applet: Applet) = _selectedReferents.any {
        it.first === applet
    }

    fun setReferentForSelections(referent: String) {
        _selectedReferents.forEach { (applet, which) ->
            referenceEditor.setReferent(applet, which, referent)
        }
    }

    private fun Flow.selectReferentWithName(name: String, block: (Applet, Int) -> Unit) {
        forEach {
            for ((index, referent) in it.referents) {
                if (referent == name) {
                    block(it, index)
                    onAppletChanged.value = it
                    break
                }
            }
            if (it is Flow) {
                it.selectReferentWithName(name, block)
            }
        }
    }

    fun selectReferent(applet: Applet, which: Int) {
        _selectedReferents.add(applet to which)
    }

    fun selectReferentsWithName(self: Applet, name: String) {
        root.selectReferentWithName(name) { applet, index ->
            if (applet != self && !self.isAttached || applet.isAheadOf(self)) {
                // Remove existed reference to this applet, because refer to multiple referents of
                // one single applet is not allowed!
                _selectedReferents.removeIf {
                    it.first === applet
                }
                _selectedReferents.add(applet to index)
            }
        }
    }

    /**
     * Remove a specific reference to an applet as per argument [which]. If [which] is default (-1),
     * remove all references to this applet.
     */
    fun unselectReferent(applet: Applet, which: Int = -1) {
        val found = _selectedReferents.find {
            it.first === applet && (which == -1 || it.second == which)
        }
        checkNotNull(found) {
            "This applet is not referred?"
        }
        val referent = found.first.referents[found.second]
        if (referent == null) {
            _selectedReferents.removeIf {
                it.first === applet
            }
            onAppletChanged.value = applet
        } else _selectedReferents.filter {
            it.first.referents[it.second] == referent
        }.forEach {
            _selectedReferents.remove(it)
            onAppletChanged.value = it.first
        }
    }

    fun isReferentLegalForSelections(referent: String): Boolean {
        return !root.forEachReferent { applet, i, name ->
            name == referent && !selectedReferents.any {
                it.first === applet && it.second == i
            }
        }
    }

    fun clearReferentSelections() {
        _selectedReferents.clear()
    }
}