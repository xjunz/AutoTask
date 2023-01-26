/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.engine.runtime.AppletIndexer
import top.xjunz.tasker.engine.task.TaskSnapshot
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.task.applet.forEachReference
import top.xjunz.tasker.task.applet.forEachReferent
import top.xjunz.tasker.task.applet.isAheadOf
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.editor.AppletReferenceEditor

/**
 * A global view model serving all [FlowEditorDialog]s, this view model is expected to
 * be hosted in Activity lifecycle.
 *
 * @author xjunz 2022/11/26
 */
class GlobalFlowEditorViewModel : ViewModel() {

    private var _root: RootFlow? = null

    private val _selectedReferents = mutableSetOf<Pair<Applet, Int>>()

    private val factory = AppletOptionFactory

    val selectedReferents: Set<Pair<Applet, Int>> get() = _selectedReferents

    val root: RootFlow get() = _root!!

    val onReferentSelected = MutableLiveData<Boolean>()

    val onAppletChanged = MutableLiveData<Applet?>()

    val referenceEditor = AppletReferenceEditor()

    val currentSnapshotIndex = MutableLiveData<Int?>()

    var isInTrackMode: Boolean = false

    val allSnapshots = MutableLiveData<Array<TaskSnapshot>>()

    var currentSnapshot: TaskSnapshot? = null
        set(value) {
            succeededApplets.clear()
            failedApplets.clear()
            if (value != null) {
                value.successes.mapTo(succeededApplets) {
                    getAppletWithHierarchy(it)
                }
                value.failures.forEach {
                    failedApplets[getAppletWithHierarchy(it.hierarchy)] = it
                }
            }
            field = value
        }

    val succeededApplets = mutableListOf<Applet>()

    val failedApplets = mutableMapOf<Applet, TaskSnapshot.Failure>()

    fun renameReferentInRoot(prev: Set<String>, cur: String?) {
        root.forEachReferent { applet, which, referent ->
            if (prev.contains(referent)) {
                referenceEditor.setReferent(applet, which, cur)
            }
            false
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

    fun generateDefaultFlow(taskType: Int): RootFlow {
        val root = factory.flowRegistry.rootFlow.yield() as RootFlow
        root.add(factory.flowRegistry.preloadFlow.yield())
        if (taskType == XTask.TYPE_RESIDENT) {
            val whenFlow = factory.flowRegistry.whenFlow.yield() as When
            whenFlow.add(factory.eventRegistry.contentChanged.yield())
            root.add(whenFlow)
        }
        root.add(factory.flowRegistry.ifFlow.yield())
        root.add(factory.flowRegistry.doFlow.yield())
        return root
    }

    fun setRoot(flow: RootFlow) {
        _root = flow
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
            if (applet != self && self.parent == null || applet.isAheadOf(self)) {
                // Remove existed reference to this applet, because multiple refs to one applet
                // is not allowed!
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
        var legal = true
        root.forEachReferent { applet, i, id ->
            if (id == referent && !selectedReferents.any {
                    it.first == applet && it.second == i
                }
            ) {
                legal = false
                true
            } else {
                false
            }
        }
        return legal
    }

    fun clearReferentSelections() {
        _selectedReferents.clear()
    }

    fun clearRootFlow() {
        _root = null
    }

    fun clearSnapshots() {
        currentSnapshot = null
        isInTrackMode = false
        allSnapshots.value = emptyArray()
        currentSnapshotIndex.value = null
    }

    private fun getAppletWithHierarchy(hierarchy: Long): Applet {
        val parsed = AppletIndexer.parse(hierarchy)
        var applet: Applet = root
        parsed.forEach { i ->
            applet = (applet as Flow)[i]
        }
        return applet
    }
}