package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.task.applet.forEachReference
import top.xjunz.tasker.task.applet.forEachRefid
import top.xjunz.tasker.task.applet.isAheadOf
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.editor.FlowReferenceTracer

/**
 * A global view model serving all [FlowEditorDialog]s, this view model is expected to
 * be hosted in Activity lifecycle.
 *
 * @author xjunz 2022/11/26
 */
class GlobalFlowEditorViewModel : ViewModel() {

    private var _root: Flow? = null

    private val selected = mutableSetOf<Pair<Applet, Int>>()

    val selectedRefs: Set<Pair<Applet, Int>> get() = selected

    val root: Flow get() = _root!!

    val factory = AppletOptionFactory()

    val onReferenceSelected = MutableLiveData<Boolean>()

    val onAppletChanged = MutableLiveData<Applet?>()

    val onNavigateFlow = MutableLiveData<Flow>()

    val tracer = FlowReferenceTracer()

    fun renameRefidInRoot(prev: Set<String>, cur: String) {
        root.forEachRefid { applet, which, refid ->
            if (prev.contains(refid)) {
                tracer.setRefid(applet, which, cur)
            }
            false
        }
        root.forEachReference { applet, which, refid ->
            if (prev.contains(refid)) {
                tracer.renameReference(applet, which, cur)
            }
            false
        }
    }

    fun setRefidForSelections(refid: String) {
        selected.forEach { (applet, which) ->
            tracer.setRefid(applet, which, refid)
        }
    }

    fun generateDefaultFlow(): Flow {
        val root = factory.flowRegistry.rootFlow.yield() as Flow
        val whenFlow = factory.flowRegistry.whenFlow.yield() as When
        whenFlow.add(factory.eventRegistry.contentChanged.yield())
        root.add(whenFlow)
        root.add(factory.flowRegistry.ifFlow.yield())
        root.add(factory.flowRegistry.doFlow.yield())
        return root
    }

    fun setRoot(flow: Flow) {
        _root = flow
    }

    fun isRefSelected(applet: Applet) = selected.any {
        it.first === applet
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
        selected.add(applet to which)
        onAppletChanged.value = applet
    }

    fun addRefSelectionWithRefid(self: Applet, refid: String) {
        root.addSelectionsWithRefid(refid) { applet, index ->
            if (applet != self && self.parent == null || applet.isAheadOf(self)) {
                selected.add(applet to index)
            }
        }
    }

    fun removeRefSelection(applet: Applet) {
        val found = selected.find {
            it.first === applet
        }
        checkNotNull(found) {
            "This applet is not referred?"
        }
        val refid = found.first.refids[found.second]
        if (refid == null) {
            selected.removeIf {
                it.first === applet
            }
            onAppletChanged.value = applet
        } else selected.filter {
            it.first.refids[it.second] == refid
        }.forEach {
            selected.remove(it)
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
        selected.clear()
    }

    fun clearRootFlow() {
        _root = null
    }
}