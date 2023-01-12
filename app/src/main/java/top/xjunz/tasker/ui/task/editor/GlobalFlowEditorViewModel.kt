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
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.task.applet.forEachReference
import top.xjunz.tasker.task.applet.forEachRefid
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

    private val _selectedRefs = mutableSetOf<Pair<Applet, Int>>()

    private val factory = AppletOptionFactory

    val selectedRefs: Set<Pair<Applet, Int>> get() = _selectedRefs

    val root: RootFlow get() = _root!!

    val onReferenceSelected = MutableLiveData<Boolean>()

    val onAppletChanged = MutableLiveData<Applet?>()

    val refEditor = AppletReferenceEditor()

    fun renameRefidInRoot(prev: Set<String>, cur: String) {
        root.forEachRefid { applet, which, refid ->
            if (prev.contains(refid)) {
                refEditor.setRefid(applet, which, cur)
            }
            false
        }
        root.forEachReference { applet, which, refid ->
            if (prev.contains(refid)) {
                refEditor.renameReference(applet, which, cur)
            }
            false
        }
    }

    fun isRefSelected(applet: Applet, which: Int): Boolean {
        return _selectedRefs.any {
            it.first === applet && it.second == which
        }
    }

    fun isRefSelected(applet: Applet) = _selectedRefs.any {
        it.first === applet
    }

    fun setRefidForSelections(refid: String) {
        _selectedRefs.forEach { (applet, which) ->
            refEditor.setRefid(applet, which, refid)
        }
    }

    fun generateDefaultFlow(taskType: Int): RootFlow {
        val root = factory.flowRegistry.rootFlow.yield() as RootFlow
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

    private fun Flow.addSelectionsWithRefid(id: String, block: (Applet, Int) -> Unit) {
        forEach {
            for ((index, refid) in it.refids) {
                if (refid == id) {
                    block(it, index)
                    onAppletChanged.value = it
                    break
                }
            }
            if (it is Flow) {
                it.addSelectionsWithRefid(id, block)
            }
        }
    }

    fun addRefSelection(applet: Applet, which: Int) {
        _selectedRefs.add(applet to which)
    }

    fun addRefSelectionWithRefid(self: Applet, refid: String) {
        root.addSelectionsWithRefid(refid) { applet, index ->
            if (applet != self && self.parent == null || applet.isAheadOf(self)) {
                // Remove existed reference to this applet, because multiple refs to one applet
                // is not allowed!
                _selectedRefs.removeIf {
                    it.first === applet
                }
                _selectedRefs.add(applet to index)
            }
        }
    }

    /**
     * Remove a specific reference to an applet as per argument [which]. If [which] is default (-1),
     * remove all references to this applet.
     */
    fun removeRefSelection(applet: Applet, which: Int = -1) {
        val found = _selectedRefs.find {
            it.first === applet && (which == -1 || it.second == which)
        }
        checkNotNull(found) {
            "This applet is not referred?"
        }
        val refid = found.first.refids[found.second]
        if (refid == null) {
            _selectedRefs.removeIf {
                it.first === applet
            }
            onAppletChanged.value = applet
        } else _selectedRefs.filter {
            it.first.refids[it.second] == refid
        }.forEach {
            _selectedRefs.remove(it)
            onAppletChanged.value = it.first
        }
    }

    fun isRefidLegalForSelections(refid: String): Boolean {
        var legal = true
        root.forEachRefid { applet, i, id ->
            if (id == refid && !selectedRefs.any {
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

    fun clearRefSelections() {
        _selectedRefs.clear()
    }

    fun clearRootFlow() {
        _root = null
    }
}